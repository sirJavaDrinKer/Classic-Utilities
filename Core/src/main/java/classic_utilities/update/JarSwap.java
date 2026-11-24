package classic_utilities.update;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


public final class JarSwap {
    private JarSwap() {
    }

    public static void main(String[] a) {
        if(a.length < 5) return;
        File tmp = a.length > 5 ? new File(a[5]) : null;
        try {
            run(new File(a[0]), new File(a[1]), new File(a[2]), new File(a[3]), new File(a[4]), 24L * 60L * 60L * 1000L, 500L);
        } finally {
            cleanup(tmp);
        }
    }

    static boolean run(File oldJar, File staged, File target, File backup, File log, long maxWaitMs, long pollMs) {
        long deadline = System.currentTimeMillis() + maxWaitMs;

        while(true) {
            if(!staged.isFile()) {
                log(log, "staged file is gone, leaving everything as it is");
                return false;
            }
            if(!oldJar.exists() || oldJar.renameTo(backup)) break;
            if(System.currentTimeMillis() > deadline) {
                log(log, "gave up waiting for " + oldJar.getName() + " to be released");
                return false;
            }
            try {
                Thread.sleep(pollMs);
            } catch(InterruptedException e) {
                return false;
            }
        }

        if(!staged.renameTo(target)) {
            if(backup.exists()) backup.renameTo(oldJar);
            log(log, "could not move " + staged.getName() + " to " + target.getName() + ", restored old jar");
            return false;
        }

        for(int i = 0; i < 10 && backup.exists(); i++) {
            if(backup.delete()) break;
            try {
                Thread.sleep(300L);
            } catch(InterruptedException e) {
                break;
            }
        }
        log(log, "update applied: " + target.getName());
        return true;
    }

    private static void log(File f, String msg) {
        PrintWriter w = null;
        try {
            w = new PrintWriter(new FileWriter(f, true));
            w.println(new java.util.Date() + " [JarSwap] " + msg);
        } catch(IOException ignored) {
        } finally {
            if(w != null) w.close();
        }
    }

    private static void cleanup(File dir) {
        if(dir == null) return;
        File pkg = new File(dir, "classic_utilities/update");
        new File(pkg, "JarSwap.class").delete();
        pkg.delete();
        pkg.getParentFile().delete();
        dir.delete();
    }
}
