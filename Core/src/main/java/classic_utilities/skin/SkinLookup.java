package classic_utilities.skin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class SkinLookup {
    public static final class Result {
        public final String textureUrl;
        public final boolean slim;

        Result(String textureUrl, boolean slim) {
            this.textureUrl = textureUrl;
            this.slim = slim;
        }

        boolean sameAs(Result other) {
            return other != null && slim == other.slim && textureUrl.equals(other.textureUrl);
        }
    }

    // name -> skin lookup, cache can be tested without a network.
    interface Resolver {
        Result resolve(String name) throws IOException;
    }

    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    private static final String[] NAME_ENDPOINTS = {
            "https://api.mojang.com/users/profiles/minecraft/",
            "https://api.minecraftservices.com/minecraft/profile/lookup/name/"
    };
    private static final String PROFILE_ENDPOINT = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String TEXTURE_HOST = "textures.minecraft.net";

    // "found" skins persist for this long, but are not verified yet
    private static final long FOUND_TTL_MS = 10L * 60L * 1000L;

    // "verified" skins persist for this long
    private static final long VERIFIED_TTL_MS = 6L * 60L * 60L * 1000L;

    // old answer can be used for this long
    private static final long MAX_STALE_MS = 7L * 24L * 60L * 60L * 1000L;

    // API returns no custom skin
    private static final long MISSING_TTL_MS = 15L * 60L * 1000L;
    private static final long ERROR_TTL_MS = 60L * 1000L;
    private static final long RATE_LIMITED_TTL_MS = 5L * 60L * 1000L;

    // don't keep hammering the API
    private static final long GLOBAL_BACKOFF_MS = 60L * 1000L;
    private static final int MAX_MEMORY_ENTRIES = 1000;
    private static final int MAX_PERSISTED_ENTRIES = 500;
    private static final String CACHE_HEADER = "# Classic Utilities skin cache v1: name, V(erified)/N(o skin), fetched at (ms), slim, url";
    private static final long REQUEST_GAP_MS = 200L;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;

    static final class RateLimitedException extends IOException {
        private static final long serialVersionUID = 1L;

        RateLimitedException(String message) {
            super(message);
        }
    }

    private static final class Entry {
        Result result;
        long validUntil;
        long fetchedAt;
        boolean busy;

        // texture downloaded and converted without problems
        boolean verified;

        // API returned no custom skin
        boolean missing;
    }

    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<String, Entry>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "CUtil skin lookup");
            t.setDaemon(true);
            return t;
        }
    });

    private final File cacheFile;
    private final Resolver resolver;
    private final AtomicBoolean dirty = new AtomicBoolean();
    private final AtomicBoolean saveQueued = new AtomicBoolean();

    private volatile boolean loggedFailure;
    private volatile boolean loggedCacheFailure;
    private volatile boolean tlsFailed;
    private volatile long rateLimitedUntil;

    public SkinLookup() {
        this(null);
    }

    public SkinLookup(File cacheFile) {
        this(cacheFile, null);
    }

    SkinLookup(File cacheFile, Resolver resolver) {
        this.cacheFile = cacheFile;
        this.resolver = resolver != null ? resolver : new Resolver() {
            public Result resolve(String name) throws IOException {
                return resolveOverNetwork(name);
            }
        };
        load();
    }

    public boolean hasTlsFailure() {
        return tlsFailed;
    }

    static boolean isTlsFailure(Throwable t) {
        for(int depth = 0; t != null && depth < 10; depth++) {
            if(t instanceof javax.net.ssl.SSLException
                    || t instanceof java.security.cert.CertificateException
                    || t instanceof java.security.cert.CertPathBuilderException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    public Result get(String name) {
        if(!isValidName(name)) return null;

        final String key = name.toLowerCase(Locale.ENGLISH);
        Entry entry = cache.get(key);
        if(entry == null) {
            if(cache.size() >= MAX_MEMORY_ENTRIES) prune();
            Entry fresh = new Entry();
            entry = cache.putIfAbsent(key, fresh);
            if(entry == null) entry = fresh;
        }

        final Entry e = entry;
        synchronized(e) {
            if(!e.busy && System.currentTimeMillis() >= e.validUntil) {
                e.busy = true;
                worker.execute(new Runnable() {
                    public void run() {
                        lookup(key, e);
                    }
                });
            }
            return e.result;
        }
    }

    public void confirm(String name, String textureUrl) {
        if(!isValidName(name) || textureUrl == null) return;

        Entry e = cache.get(name.toLowerCase(Locale.ENGLISH));
        if(e == null) return;

        synchronized(e) {
            if(e.verified || e.result == null || !textureUrl.equals(e.result.textureUrl)) return;
            e.verified = true;
            e.validUntil = Math.max(e.validUntil, e.fetchedAt + VERIFIED_TTL_MS);
        }
        scheduleSave();
    }

    static boolean isValidName(String name) {
        if(name == null || name.length() < 1 || name.length() > 16) return false;
        for(int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
            if(!ok) return false;
        }
        return true;
    }

    private void lookup(String name, Entry e) {
        Result result = null;
        long ttl;
        boolean keepOld = false;
        boolean usedNetwork = true;

        long now = System.currentTimeMillis();
        if(now < rateLimitedUntil) {
            usedNetwork = false;
            keepOld = true;
            ttl = Math.max(1000L, rateLimitedUntil - now);
        } else {
            try {
                result = resolver.resolve(name);
                ttl = result != null ? FOUND_TTL_MS : MISSING_TTL_MS;
            } catch(RateLimitedException ex) {
                ttl = RATE_LIMITED_TTL_MS;
                keepOld = true;
                rateLimitedUntil = System.currentTimeMillis() + GLOBAL_BACKOFF_MS;
                logFailure(name, ex);
            } catch(Exception ex) {
                ttl = ERROR_TTL_MS;
                keepOld = true;
                if(isTlsFailure(ex)) tlsFailed = true;
                logFailure(name, ex);
            }
        }

        now = System.currentTimeMillis();
        synchronized(e) {
            if(!keepOld) {
                boolean stillVerified = result != null && e.verified && result.sameAs(e.result);
                e.result = result;
                e.missing = result == null;
                e.verified = stillVerified;
                e.fetchedAt = now;
                if(stillVerified) ttl = VERIFIED_TTL_MS;
            }
            e.validUntil = now + ttl;
            e.busy = false;
        }
        if(!keepOld) scheduleSave();

        if(usedNetwork) {
            try {
                Thread.sleep(REQUEST_GAP_MS);
            } catch(InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void logFailure(String name, Exception ex) {
        if(!loggedFailure) {
            loggedFailure = true;
            LOG.log(Level.WARNING, "[CUtil] skin lookup for " + name + " failed (will retry later): " + ex);
        }
    }

    private void prune() {
        long now = System.currentTimeMillis();
        for(Map.Entry<String, Entry> me : cache.entrySet()) {
            Entry e = me.getValue();
            synchronized(e) {
                // Verified answers are what the cache file is made of, keep those.
                if(!e.busy && !e.verified && now >= e.validUntil) cache.remove(me.getKey(), e);
            }
        }
    }

    private void scheduleSave() {
        if(cacheFile == null) return;
        dirty.set(true);
        if(saveQueued.compareAndSet(false, true)) {
            worker.execute(new Runnable() {
                public void run() {
                    saveQueued.set(false);
                    if(dirty.getAndSet(false)) save();
                }
            });
        }
    }

    private static final class Row {
        final long fetchedAt;
        final String line;

        Row(long fetchedAt, String line) {
            this.fetchedAt = fetchedAt;
            this.line = line;
        }
    }

    private void save() {
        long now = System.currentTimeMillis();
        List<Row> rows = new ArrayList<Row>();
        for(Map.Entry<String, Entry> me : cache.entrySet()) {
            Entry e = me.getValue();
            synchronized(e) {
                if(e.fetchedAt <= 0) continue;
                long age = now - e.fetchedAt;
                if(e.missing && age < MISSING_TTL_MS) {
                    rows.add(new Row(e.fetchedAt, me.getKey() + "\tN\t" + e.fetchedAt + "\t0\t-"));
                } else if(e.verified && e.result != null && age < MAX_STALE_MS && isPlainLine(e.result.textureUrl)) {
                    rows.add(new Row(e.fetchedAt, me.getKey() + "\tV\t" + e.fetchedAt + "\t" + (e.result.slim ? "1" : "0")
                            + "\t" + e.result.textureUrl));
                }
            }
        }

        Collections.sort(rows, new Comparator<Row>() {
            public int compare(Row a, Row b) {
                return a.fetchedAt < b.fetchedAt ? 1 : a.fetchedAt > b.fetchedAt ? -1 : 0;
            }
        });

        File tmp = new File(cacheFile.getPath() + ".tmp");
        BufferedWriter out = null;
        try {
            File parent = cacheFile.getAbsoluteFile().getParentFile();
            if(parent != null && !parent.exists()) parent.mkdirs();

            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8"));
            out.write(CACHE_HEADER);
            out.write("\n");
            for(int i = 0; i < rows.size() && i < MAX_PERSISTED_ENTRIES; i++) {
                out.write(rows.get(i).line);
                out.write("\n");
            }
            out.close();
            out = null;

            if(!tmp.renameTo(cacheFile)) {
                cacheFile.delete();
                if(!tmp.renameTo(cacheFile)) throw new IOException("could not replace " + cacheFile);
            }
        } catch(IOException ex) {
            logCacheFailure("write", ex);
        } finally {
            if(out != null) {
                try {
                    out.close();
                } catch(IOException ignored) {
                }
            }
        }
    }

    private void load() {
        if(cacheFile == null || !cacheFile.isFile()) return;

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile), "UTF-8"));
            long now = System.currentTimeMillis();
            String line;
            int lines = 0;
            while((line = in.readLine()) != null && lines++ < MAX_PERSISTED_ENTRIES * 4) {
                parseLine(line, now);
            }
        } catch(IOException ex) {
            logCacheFailure("read", ex);
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch(IOException ignored) {
                }
            }
        }
    }

    private void parseLine(String line, long now) {
        if(line.length() == 0 || line.charAt(0) == '#') return;

        String[] f = line.split("\t", -1);
        if(f.length != 5 || !isValidName(f[0])) return;

        long fetchedAt;
        try {
            fetchedAt = Long.parseLong(f[2]);
        } catch(NumberFormatException ex) {
            return;
        }
        if(fetchedAt <= 0) return;
        if(fetchedAt > now) fetchedAt = now;
        long age = now - fetchedAt;

        Entry e = new Entry();
        e.fetchedAt = fetchedAt;
        if("N".equals(f[1])) {
            if(age >= MISSING_TTL_MS) return;
            e.missing = true;
            e.validUntil = fetchedAt + MISSING_TTL_MS;
        } else if("V".equals(f[1])) {
            if(age >= MAX_STALE_MS) return;
            if(!"0".equals(f[3]) && !"1".equals(f[3])) return;
            if(!isTrustedTextureUrl(f[4])) return;
            e.result = new Result(f[4], "1".equals(f[3]));
            e.verified = true;
            e.validUntil = fetchedAt + VERIFIED_TTL_MS;
        } else {
            return;
        }
        cache.put(f[0].toLowerCase(Locale.ENGLISH), e);
    }

    private static boolean isPlainLine(String s) {
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if(c == '\t' || c == '\n' || c == '\r') return false;
        }
        return true;
    }

    private void logCacheFailure(String action, IOException ex) {
        if(!loggedCacheFailure) {
            loggedCacheFailure = true;
            LOG.log(Level.WARNING, "[CUtil] could not " + action + " the skin cache " + cacheFile + ": " + ex);
        }
    }

    private Result resolveOverNetwork(String name) throws IOException {
        String uuid = findUuid(name);
        if(uuid == null) return null;

        String profile = httpGet(PROFILE_ENDPOINT + uuid);
        if(profile == null) return null;

        int textures = profile.indexOf("\"textures\"");
        if(textures < 0) return null;
        String encoded = stringValue(profile, "value", textures);
        if(encoded == null) return null;

        String json = new String(decodeBase64(encoded), "UTF-8");
        return parseTextures(json);
    }

    private String findUuid(String name) throws IOException {
        IOException last = null;
        for(int i = 0; i < NAME_ENDPOINTS.length; i++) {
            try {
                String body = httpGet(NAME_ENDPOINTS[i] + name);
                if(body == null) return null;

                String id = stringValue(body, "id", 0);
                if(id != null && id.matches("[0-9a-fA-F]{32}")) return id;
                return null;
            } catch(RateLimitedException ex) {
                throw ex;
            } catch(IOException ex) {
                last = ex;
            }
        }
        throw last != null ? last : new IOException("no name lookup endpoint available");
    }

    static Result parseTextures(String json) {
        int skin = json.indexOf("\"SKIN\"");
        if(skin < 0) return null;

        int open = json.indexOf('{', skin);
        if(open < 0) return null;
        int close = matchingBrace(json, open);
        if(close < 0) return null;
        String block = json.substring(open, close + 1);

        String url = stringValue(block, "url", 0);
        if(url == null || !isTrustedTextureUrl(url)) return null;

        String model = stringValue(block, "model", 0);
        return new Result(url, "slim".equals(model));
    }

    private static boolean isTrustedTextureUrl(String url) {
        try {
            URL u = new URL(url);
            String protocol = u.getProtocol();
            return ("http".equals(protocol) || "https".equals(protocol)) && TEXTURE_HOST.equalsIgnoreCase(u.getHost());
        } catch(IOException e) {
            return false;
        }
    }

    private static int matchingBrace(String s, int open) {
        int depth = 0;
        boolean inString = false;
        for(int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if(inString) {
                if(c == '\\') i++;
                else if(c == '"') inString = false;
            } else if(c == '"') {
                inString = true;
            } else if(c == '{') {
                depth++;
            } else if(c == '}') {
                if(--depth == 0) return i;
            }
        }
        return -1;
    }

    static String stringValue(String json, String key, int from) {
        String needle = "\"" + key + "\"";
        int at = json.indexOf(needle, from);
        while(at >= 0) {
            int i = at + needle.length();
            while(i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if(i < json.length() && json.charAt(i) == ':') {
                i++;
                while(i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
                if(i < json.length() && json.charAt(i) == '"') return readString(json, i + 1);
            }
            at = json.indexOf(needle, at + 1);
        }
        return null;
    }

    private static String readString(String s, int start) {
        StringBuilder out = new StringBuilder();
        for(int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if(c == '"') return out.toString();
            if(c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch(n) {
                    case 'n': out.append('\n'); break;
                    case 't': out.append('\t'); break;
                    case 'r': out.append('\r'); break;
                    case 'u':
                        if(i + 4 < s.length()) {
                            try {
                                out.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                            } catch(NumberFormatException ignored) {
                            }
                            i += 4;
                        }
                        break;
                    default: out.append(n);
                }
            } else {
                out.append(c);
            }
        }
        return null;
    }

    static byte[] decodeBase64(String s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(s.length() * 3 / 4);
        int buffer = 0;
        int bits = 0;
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int v;
            if(c >= 'A' && c <= 'Z') v = c - 'A';
            else if(c >= 'a' && c <= 'z') v = c - 'a' + 26;
            else if(c >= '0' && c <= '9') v = c - '0' + 52;
            else if(c == '+' || c == '-') v = 62;
            else if(c == '/' || c == '_') v = 63;
            else continue; // padding and whitespace

            buffer = buffer << 6 | v;
            bits += 6;
            if(bits >= 8) {
                bits -= 8;
                out.write(buffer >> bits & 0xFF);
            }
        }
        return out.toByteArray();
    }

    private static String httpGet(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "ClassicUtilities/2.1");
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if(code == 204 || code == 404) return null;
            if(code == 429) throw new RateLimitedException("HTTP 429 from " + url);
            if(code != 200) throw new IOException("HTTP " + code + " from " + url);

            InputStream in = conn.getInputStream();
            try {
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while((n = in.read(buf)) != -1) {
                    body.write(buf, 0, n);
                    if(body.size() > 1024 * 1024) throw new IOException("response too large");
                }
                return new String(body.toByteArray(), "UTF-8");
            } finally {
                try {
                    in.close();
                } catch(IOException ignored) {
                }
            }
        } finally {
            conn.disconnect();
        }
    }
}