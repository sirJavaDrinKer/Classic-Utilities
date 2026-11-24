package classic_utilities.update;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;

public final class UpdateChecker {
    public static final String MOD_PAGE = "https://modrinth.com/mod/classic-utilities";

    private static final String PROJECT_ID = "4P0rRD7P";
    private static final String API = "https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version";
    private static final String TRUSTED_DOWNLOAD_HOST = "cdn.modrinth.com";

    private static final Logger LOG = Logger.getLogger("ForgeModLoader");
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    private static final Pattern SAFE_VERSION = Pattern.compile("[0-9A-Za-z._+-]{1,40}");
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("[A-Za-z0-9._+()-]{1,100}");
    private static final Pattern SHA512 = Pattern.compile("[0-9a-fA-F]{128}");

    private static final Pattern V_TOKEN = Pattern.compile("(?:^|[^0-9A-Za-z])[vV](\\d+(?:\\.\\d+)*)");
    private static final Pattern NUM_TOKEN = Pattern.compile("\\d+(?:\\.\\d+)*");

    public static final class Release {
        public final String version;
        public final String fileName;
        final String url;
        final String sha512;
        final long size;

        Release(String version, String fileName, String url, String sha512, long size) {
            this.version = version;
            this.fileName = fileName;
            this.url = url;
            this.sha512 = sha512;
            this.size = size;
        }
    }

    private static final AtomicBoolean started = new AtomicBoolean();
    private static volatile Release available;
    private static volatile String currentVersion = "unknown";

    private UpdateChecker() {
    }

    public static void start(final String version, final String mcVersion) {
        if(version == null || version.indexOf("${") >= 0) return;
        if(mcVersion == null || !mcVersion.matches("[0-9A-Za-z._-]+")) return;
        if(!started.compareAndSet(false, true)) return;
        currentVersion = version;

        Thread t = new Thread(new Runnable() {
            public void run() {
                check(version, mcVersion);
            }
        }, "CUtil update check");
        t.setDaemon(true);
        t.start();
    }

    public static Release getAvailable() {
        return available;
    }

    static String userAgent() {
        return "javaDrinker/classic-utilities/" + currentVersion + " (update check)";
    }

    private static void check(String current, String mcVersion) {
        try {
            List<String> skipped = new ArrayList<String>();
            Release latest = latestRelease(fetch(buildUrl(mcVersion)), mcVersion, skipped);
            if(latest == null) {
                LOG.info("[CUtil] update check: no usable release for Minecraft " + mcVersion + (skipped.isEmpty()
                        ? " (Modrinth returned no versions tagged forge + " + mcVersion + ")"
                        : "; skipped: " + join(skipped)));
            } else if(isNewer(latest.version, current)) {
                LOG.info("[CUtil] update available: " + latest.version + " (running " + current + ")");
                available = latest;
            } else {
                LOG.info("[CUtil] update check: up to date (latest release " + latest.version + ", running " + current + ")");
            }
        } catch(SSLException e) {
            LOG.info("[CUtil] update check skipped, no secure connection to Modrinth: " + e.getMessage());
        } catch(IOException e) {
            LOG.info("[CUtil] update check failed: " + e);
        } catch(RuntimeException e) {
            LOG.log(Level.WARNING, "[CUtil] update check crashed", e);
        }
    }

    static String buildUrl(String mcVersion) {
        return API + "?loaders=%5B%22forge%22%5D&game_versions=%5B%22" + mcVersion + "%22%5D";
    }

    private static String fetch(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(CONNECT_TIMEOUT_MS);
        c.setReadTimeout(READ_TIMEOUT_MS);
        c.setRequestProperty("User-Agent", userAgent());
        c.setRequestProperty("Accept", "application/json");

        InputStream in = null;
        try {
            int code = c.getResponseCode();
            if(code != 200) throw new IOException("HTTP " + code);

            in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                if(out.size() > MAX_RESPONSE_BYTES) throw new IOException("response too large");
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch(IOException ignored) {
                }
            }
            c.disconnect();
        }
    }

    static Release latestRelease(String json, String mcVersion) throws IOException {
        return latestRelease(json, mcVersion, new ArrayList<String>());
    }

    static Release latestRelease(String json, String mcVersion, List<String> skipped) throws IOException {
        List<Object> versions = MiniJson.list(MiniJson.parse(json));
        if(versions == null) throw new IOException("unexpected response shape");

        Release best = null;
        for(Object o : versions) {
            Release r = toRelease(MiniJson.obj(o), mcVersion, skipped);
            if(r != null && (best == null || isNewer(r.version, best.version))) best = r;
        }
        return best;
    }

    private static Release toRelease(Map<String, Object> v, String mcVersion, List<String> skipped) {
        if(v == null) return null;
        String version = MiniJson.str(v.get("version_number"));
        String label = "'" + (version == null ? "?" : version) + "'";

        String type = MiniJson.str(v.get("version_type"));
        if(!"release".equals(type)) {
            skipped.add(label + " is type " + type + ", not release");
            return null;
        }

        List<Object> games = MiniJson.list(v.get("game_versions"));
        if(games == null || !games.contains(mcVersion)) {
            skipped.add(label + " is tagged " + games + ", not " + mcVersion);
            return null;
        }

        if(version == null || !SAFE_VERSION.matcher(version).matches() || parse(version) == null) {
            skipped.add(label + " has an unusable version number");
            return null;
        }

        List<Object> files = MiniJson.list(v.get("files"));
        Map<String, Object> file = null;
        if(files != null) {
            for(Object f : files) {
                Map<String, Object> m = MiniJson.obj(f);
                if(m == null) continue;
                if(file == null) file = m;
                if(Boolean.TRUE.equals(m.get("primary"))) {
                    file = m;
                    break;
                }
            }
        }
        if(file == null) {
            skipped.add(label + " has no files");
            return null;
        }

        String url = MiniJson.str(file.get("url"));
        if(url == null || !isTrustedDownloadUrl(url)) {
            skipped.add(label + " is not hosted on " + TRUSTED_DOWNLOAD_HOST);
            return null;
        }

        Map<String, Object> hashes = MiniJson.obj(file.get("hashes"));
        String sha512 = hashes == null ? null : MiniJson.str(hashes.get("sha512"));
        if(sha512 == null || !SHA512.matcher(sha512).matches()) {
            skipped.add(label + " has no sha512 hash");
            return null;
        }

        Object size = file.get("size");
        long bytes = size instanceof Number ? ((Number) size).longValue() : -1L;

        return new Release(version, safeFileName(MiniJson.str(file.get("filename")), version), url,
                sha512.toLowerCase(Locale.ROOT), bytes);
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < parts.size(); i++) {
            if(i > 0) sb.append("; ");
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    static boolean isTrustedDownloadUrl(String url) {
        try {
            URL u = new URL(url);
            return "https".equals(u.getProtocol())
                    && TRUSTED_DOWNLOAD_HOST.equalsIgnoreCase(u.getHost())
                    && (u.getPort() == -1 || u.getPort() == 443)
                    && u.getUserInfo() == null;
        } catch(MalformedURLException e) {
            return false;
        }
    }

    static String safeFileName(String name, String version) {
        if(name != null && SAFE_FILE_NAME.matcher(name).matches()
                && name.toLowerCase(Locale.ROOT).endsWith(".jar")
                && !name.startsWith(".") && name.indexOf("..") < 0) {
            return name;
        }
        return "classic_utilities-" + version + ".jar";
    }

    static boolean isNewer(String remote, String local) {
        int[] r = parse(remote);
        int[] l = parse(local);
        if(r == null || l == null) return false;

        int n = Math.max(r.length, l.length);
        for(int i = 0; i < n; i++) {
            int a = i < r.length ? r[i] : 0;
            int b = i < l.length ? l[i] : 0;
            if(a != b) return a > b;
        }
        return false;
    }

    static int[] parse(String version) {
        if(version == null) return null;
        String v = version.trim();

        String core = null;
        Matcher m = V_TOKEN.matcher(v);
        if(m.find()) {
            core = m.group(1);
        } else if(v.length() > 0 && Character.isDigit(v.charAt(0))) {
            int end = 0;
            while(end < v.length() && (Character.isDigit(v.charAt(end)) || v.charAt(end) == '.')) end++;
            core = v.substring(0, end);
        } else {
            Matcher n = NUM_TOKEN.matcher(v);
            while(n.find()) core = n.group();
        }
        if(core == null || core.length() == 0) return null;

        String[] parts = core.split("\\.");
        int[] out = new int[parts.length];
        try {
            for(int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        } catch(NumberFormatException e) {
            return null;
        }
        return out;
    }

    static void setAvailableForTest(Release r) {
        available = r;
    }
}
