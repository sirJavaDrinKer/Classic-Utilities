package classic_utilities.forge.skin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ModernSkins {
    private static final ConcurrentMap<String, Boolean> READY = new ConcurrentHashMap<String, Boolean>();

    private ModernSkins() {
    }

    static void markReady(String url, boolean slim) {
        READY.put(url, Boolean.valueOf(slim));
    }

    public static void forget(String url) {
        if(url != null) READY.remove(url);
    }

    public static Boolean slimFor(String url) {
        return url == null ? null : READY.get(url);
    }
}
