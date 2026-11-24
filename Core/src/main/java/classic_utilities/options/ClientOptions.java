package classic_utilities.options;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClientOptions {
    public static final int BETTER_HITBOXES = 0;
    public static final int SKIN_FIX = 1;
    public static final int COUNT = 2;

    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    private static final boolean[] DEFAULTS = { true, false };

    private static final boolean[] values = DEFAULTS.clone();
    private static File file;

    private ClientOptions() {
    }

    public static synchronized void load(File saveFile) {
        file = saveFile;
        System.arraycopy(DEFAULTS, 0, values, 0, COUNT);

        if(file == null || !file.isFile()) return;

        Properties props = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            props.load(in);
            for(int id = 0; id < COUNT; id++) {
                String value = props.getProperty(key(id));
                if(value != null) values[id] = Boolean.parseBoolean(value.trim());
            }
        } catch(IOException e) {
            LOG.log(Level.WARNING, "[CUtil] could not read client options, using defaults", e);
        } finally {
            close(in);
        }
    }

    public static synchronized File siblingFile(String name) {
        if(file == null) return null;
        File parent = file.getAbsoluteFile().getParentFile();
        return parent == null ? new File(name) : new File(parent, name);
    }

    public static synchronized boolean get(int id) {
        check(id);
        return values[id];
    }

    public static synchronized void set(int id, boolean value) {
        check(id);
        if(values[id] == value) return;
        values[id] = value;
        save();
    }

    public static void toggle(int id) {
        set(id, !get(id));
    }

    public static boolean isBetterHitboxes() {
        return get(BETTER_HITBOXES);
    }

    public static boolean isSkinFix() {
        return get(SKIN_FIX);
    }

    public static boolean isValidId(int id) {
        return id >= 0 && id < COUNT;
    }

    public static String label(int id) {
        switch(id) {
            case BETTER_HITBOXES: return "Better Hitboxes";
            case SKIN_FIX: return "Skin Fix";
            default: throw new IllegalArgumentException("Unknown option id " + id);
        }
    }

    public static String key(int id) {
        switch(id) {
            case BETTER_HITBOXES: return "betterHitboxes";
            case SKIN_FIX: return "skinFix";
            default: throw new IllegalArgumentException("Unknown option id " + id);
        }
    }

    private static void check(int id) {
        if(!isValidId(id)) throw new IllegalArgumentException("Unknown option id " + id);
    }

    private static void save() {
        if(file == null) return;

        Properties props = new Properties();
        for(int id = 0; id < COUNT; id++) {
            props.setProperty(key(id), Boolean.toString(values[id]));
        }

        FileOutputStream out = null;
        try {
            File parent = file.getParentFile();
            if(parent != null && !parent.exists()) parent.mkdirs();
            out = new FileOutputStream(file);
            props.store(out, "Classic Utilities client options");
        } catch(IOException e) {
            LOG.log(Level.WARNING, "[CUtil] could not save client options", e);
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
}
