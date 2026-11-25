package classic_utilities.update;

import classic_utilities.options.ClientOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UpdateConsent {
    private static final String FILE_NAME = "classic_utilities_update.properties";
    private static final String KEY = "updateWarningAccepted";

    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    private static boolean loaded;
    private static boolean accepted;

    private UpdateConsent() {
    }

    public static synchronized boolean isAccepted() {
        if(!loaded) load();
        return accepted;
    }

    public static synchronized void accept() {
        accepted = true;
        loaded = true;
        save();
    }

    private static void load() {
        File file = ClientOptions.siblingFile(FILE_NAME);
        if(file == null) return;

        loaded = true;
        accepted = false;
        if(!file.isFile()) return;

        Properties props = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            props.load(in);
            accepted = Boolean.parseBoolean(props.getProperty(KEY, "false").trim());
        } catch(IOException e) {
            LOG.log(Level.WARNING, "[CUtil] could not read update settings, the warning will be shown again", e);
        } finally {
            close(in);
        }
    }

    private static void save() {
        File file = ClientOptions.siblingFile(FILE_NAME);
        if(file == null) {
            LOG.warning("[CUtil] no config folder, update warning acceptance will not be remembered");
            return;
        }

        Properties props = new Properties();
        props.setProperty(KEY, Boolean.toString(accepted));

        FileOutputStream out = null;
        try {
            File parent = file.getParentFile();
            if(parent != null && !parent.exists()) parent.mkdirs();
            out = new FileOutputStream(file);
            props.store(out, "Classic Utilities update settings");
        } catch(IOException e) {
            LOG.log(Level.WARNING, "[CUtil] could not save update settings, the warning will be shown again next launch", e);
        } finally {
            close(out);
        }
    }

    private static void close(java.io.Closeable c) {
        if(c == null) return;
        try {
            c.close();
        } catch(IOException ignored) {
        }
    }

    static synchronized void resetForTest() {
        loaded = false;
        accepted = false;
    }
}
