package classic_utilities.util;

public final class JavaVersion {

    public static final int MIN_MAJOR = 8;

    public static final int MIN_UPDATE_OF_MIN_MAJOR = 0;

    private JavaVersion() {
    }

    public static String current() {
        return System.getProperty("java.version", "unknown");
    }

    public static boolean isTooOld() {
        return isTooOld(current());
    }

    static boolean isTooOld(String version) {
        int major = major(version);
        if(major < 0) return false;
        if(major < MIN_MAJOR) return true;
        return major == MIN_MAJOR && MIN_UPDATE_OF_MIN_MAJOR > 0 && update(version) < MIN_UPDATE_OF_MIN_MAJOR;
    }

    static int major(String version) {
        if(version == null) return -1;
        String[] parts = version.trim().split("[._\\-+]");
        try {
            int first = Integer.parseInt(parts[0]);
            if(first == 1 && parts.length > 1) return Integer.parseInt(parts[1]);
            return first;
        } catch(NumberFormatException e) {
            return -1;
        }
    }

    static int update(String version) {
        int at = version.indexOf('_');
        if(at < 0) return 0;
        int end = at + 1;
        while(end < version.length() && Character.isDigit(version.charAt(end))) end++;
        try {
            return Integer.parseInt(version.substring(at + 1, end));
        } catch(NumberFormatException e) {
            return 0;
        }
    }
}