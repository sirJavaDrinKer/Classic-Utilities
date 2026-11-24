package classic_utilities.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

public final class UpdateInstaller {
    public enum State { IDLE, DOWNLOADING, SUCCESS, FAILED }

    enum Outcome { INSTALLED, DEFERRED }

    interface Fs {
        boolean rename(File from, File to);

        boolean delete(File f);
    }

    interface Deferred {
        boolean spawn(File oldJar, File staged, File target, File backup);
    }

    private static final Logger LOG = Logger.getLogger("ForgeModLoader");
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 20000;
    private static final long MAX_DOWNLOAD_BYTES = 32L * 1024L * 1024L;

    static final Fs REAL_FS = new Fs() {
        public boolean rename(File from, File to) {
            return from.renameTo(to);
        }

        public boolean delete(File f) {
            return f.delete();
        }
    };

    private static final Deferred HELPER = new Deferred() {
        public boolean spawn(File oldJar, File staged, File target, File backup) {
            return spawnHelper(oldJar, staged, target, backup);
        }
    };

    private static volatile State state = State.IDLE;

    private UpdateInstaller() {
    }


    public static State getState() {
        return state;
    }

    public static String statusText() {
        switch(state) {
            case DOWNLOADING:
                return "Updating...";
            case SUCCESS:
                return "Update successful! Restart to see changes.";
            case FAILED:
                return "Failed to update; click here to open mod page.";
            default:
                return UpdateChecker.getAvailable() != null ? "Update available, click here to update." : null;
        }
    }

    public static boolean isClickable() {
        return state == State.FAILED || (state == State.IDLE && UpdateChecker.getAvailable() != null);
    }

    public static void click() {
        State s = state;
        if(s == State.FAILED) openModPage();
        else if(s == State.IDLE) start();
    }


    public static boolean start() {
        return startWith(UpdateChecker.getAvailable(), locateRunningJar(), REAL_FS, HELPER);
    }

    static synchronized boolean startWith(final UpdateChecker.Release release, final File jar, final Fs fs, final Deferred deferred) {
        if(state != State.IDLE || release == null) return false;
        state = State.DOWNLOADING;

        Thread t = new Thread(new Runnable() {
            public void run() {
                State result = State.FAILED;
                try {
                    Outcome o = install(release, jar, fs, deferred);
                    LOG.info("[CUtil] update to " + release.version + (o == Outcome.DEFERRED
                            ? " downloaded; it will be applied when the game closes"
                            : " installed; restart to use it"));
                    result = State.SUCCESS;
                } catch(IOException e) {
                    LOG.warning("[CUtil] update failed: " + e.getMessage());
                } catch(Throwable e) {
                    LOG.log(Level.WARNING, "[CUtil] update crashed", e);
                }
                state = result;
            }
        }, "CUtil updater");
        t.setDaemon(true);
        t.start();
        return true;
    }

    static Outcome install(UpdateChecker.Release r, File currentJar, Fs fs, Deferred deferred) throws IOException {
        if(currentJar == null || !currentJar.isFile()) throw new IOException("running jar not found (dev environment?)");
        File dir = currentJar.getAbsoluteFile().getParentFile();
        if(dir == null || !dir.isDirectory()) throw new IOException("mods folder not found");

        File target = new File(dir, r.fileName);
        File staged = new File(dir, r.fileName + ".update");
        File backup = new File(dir, currentJar.getName() + ".old");

        if(target.exists() && !sameFile(target, currentJar)) {
            throw new IOException(target.getName() + " already exists in the mods folder");
        }

        boolean verified = false;
        try {
            download(r, staged);
            verifyJar(staged);
            verified = true;
        } finally {
            if(!verified) staged.delete();
        }

        if(backup.exists() && !fs.delete(backup)) {
            staged.delete();
            throw new IOException("could not clear leftover " + backup.getName());
        }

        if(!fs.rename(currentJar, backup)) {
            if(deferred != null && deferred.spawn(currentJar, staged, target, backup)) return Outcome.DEFERRED;
            staged.delete();
            throw new IOException("the running jar is locked and the helper could not start");
        }

        if(!fs.rename(staged, target)) {
            boolean restored = fs.rename(backup, currentJar);
            staged.delete();
            throw new IOException("could not move the new jar into place"
                    + (restored ? "" : " and could not restore the old one (it is saved as " + backup.getName() + ")"));
        }

        fs.delete(backup);
        return Outcome.INSTALLED;
    }

    private static void download(UpdateChecker.Release r, File dest) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch(NoSuchAlgorithmException e) {
            throw new IOException("SHA-512 is not available");
        }

        HttpURLConnection c = (HttpURLConnection) new URL(r.url).openConnection();
        c.setConnectTimeout(CONNECT_TIMEOUT_MS);
        c.setReadTimeout(READ_TIMEOUT_MS);
        c.setRequestProperty("User-Agent", UpdateChecker.userAgent());

        InputStream in = null;
        OutputStream out = null;
        long total = 0;
        try {
            int code = c.getResponseCode();
            if(code != 200) throw new IOException("download failed: HTTP " + code);
            if(c.getContentLength() > MAX_DOWNLOAD_BYTES) throw new IOException("download too large");

            in = c.getInputStream();
            out = new FileOutputStream(dest);
            byte[] buf = new byte[16384];
            int n;
            while((n = in.read(buf)) != -1) {
                total += n;
                if(total > MAX_DOWNLOAD_BYTES) throw new IOException("download too large");
                md.update(buf, 0, n);
                out.write(buf, 0, n);
            }
            out.close();
            out = null;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            c.disconnect();
        }

        if(r.size >= 0 && total != r.size) throw new IOException("download size mismatch");
        if(!hex(md.digest()).equalsIgnoreCase(r.sha512)) throw new IOException("download checksum mismatch");
    }

    private static void verifyJar(File f) throws IOException {
        ZipFile z = null;
        try {
            z = new ZipFile(f);
            if(z.getEntry("mcmod.info") == null) throw new IOException("downloaded file is not a mod jar");
        } catch(java.util.zip.ZipException e) {
            throw new IOException("downloaded file is not a valid jar");
        } finally {
            if(z != null) {
                try {
                    z.close();
                } catch(IOException ignored) {
                }
            }
        }
    }

    static File locateRunningJar() {
        return locateJar(UpdateInstaller.class);
    }

    static File locateJar(Class<?> c) {
        StringBuilder tried = new StringBuilder();

        try {
            CodeSource cs = c.getProtectionDomain().getCodeSource();
            URL loc = cs == null ? null : cs.getLocation();
            tried.append("code source ").append(loc);
            File f = fileFromUrl(loc);
            if(f != null) return f;
        } catch(Exception e) {
            tried.append("code source failed: ").append(e);
        }

        try {
            URL res = c.getResource(c.getSimpleName() + ".class");
            tried.append("; class resource ").append(res);
            File f = fileFromUrl(res);
            if(f != null) return f;
        } catch(Exception e) {
            tried.append("; class resource failed: ").append(e);
        }

        LOG.warning("[CUtil] could not locate the running jar (" + tried + ")");
        return null;
    }

    static File fileFromUrl(URL u) {
        if(u == null) return null;
        try {
            String protocol = u.getProtocol();
            if("jar".equals(protocol)) {
                String spec = u.getPath();
                int bang = spec.indexOf("!/");
                if(bang >= 0) spec = spec.substring(0, bang);
                return fileFromUrl(new URL(spec));
            }
            if(!"file".equals(protocol)) return null;

            File f;
            try {
                f = new File(u.toURI());
            } catch(Exception e) {
                f = new File(URLDecoder.decode(u.getPath().replace("+", "%2B"), "UTF-8"));
            }
            return f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".jar") ? f : null;
        } catch(Exception e) {
            return null;
        }
    }

    private static boolean sameFile(File a, File b) {
        try {
            return a.getCanonicalFile().equals(b.getCanonicalFile());
        } catch(IOException e) {
            return a.getAbsoluteFile().equals(b.getAbsoluteFile());
        }
    }


    private static boolean spawnHelper(File oldJar, File staged, File target, File backup) {
        InputStream in = null;
        OutputStream out = null;
        try {
            File dir = File.createTempFile("cutil-swap", "");
            if(!dir.delete() || !dir.mkdirs()) return false;
            File cls = new File(dir, "classic_utilities/update/JarSwap.class");
            if(!cls.getParentFile().mkdirs()) return false;

            in = UpdateInstaller.class.getResourceAsStream("/classic_utilities/update/JarSwap.class");
            if(in == null) return false;
            out = new FileOutputStream(cls);
            byte[] buf = new byte[4096];
            int n;
            while((n = in.read(buf)) != -1) out.write(buf, 0, n);
            out.close();
            out = null;

            File log = new File(System.getProperty("java.io.tmpdir"), "cutil-update.log");
            ProcessBuilder pb = new ProcessBuilder(javaExecutable(), "-cp", dir.getAbsolutePath(),
                    "classic_utilities.update.JarSwap",
                    oldJar.getAbsolutePath(), staged.getAbsolutePath(), target.getAbsolutePath(),
                    backup.getAbsolutePath(), log.getAbsolutePath(), dir.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getOutputStream().close();
            return true;
        } catch(Exception e) {
            LOG.log(Level.WARNING, "[CUtil] could not start the update helper", e);
            return false;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    private static String javaExecutable() {
        File bin = new File(System.getProperty("java.home"), "bin");
        File javaw = new File(bin, "javaw.exe");
        if(javaw.isFile()) return javaw.getPath();
        File exe = new File(bin, "java.exe");
        if(exe.isFile()) return exe.getPath();
        return new File(bin, "java").getPath();
    }


    public static void openModPage() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                browse(UpdateChecker.MOD_PAGE);
            }
        }, "CUtil open page");
        t.setDaemon(true);
        t.start();
    }

    private static void browse(String url) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if(os.contains("win")) Runtime.getRuntime().exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", url });
            else if(os.contains("mac")) Runtime.getRuntime().exec(new String[] { "open", url });
            else Runtime.getRuntime().exec(new String[] { "xdg-open", url });
            return;
        } catch(IOException e) {

        }
        try {
            if(java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch(Throwable e) {
            LOG.log(Level.WARNING, "[CUtil] could not open " + url, e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for(int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            if(b < 16) sb.append('0');
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }

    private static void closeQuietly(java.io.Closeable c) {
        if(c == null) return;
        try {
            c.close();
        } catch(IOException ignored) {
        }
    }

    static void resetForTest() {
        state = State.IDLE;
        UpdateChecker.setAvailableForTest(null);
    }
}
