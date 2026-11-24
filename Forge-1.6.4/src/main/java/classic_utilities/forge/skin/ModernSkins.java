package classic_utilities.forge.skin;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ModernSkins {
    private static final ConcurrentMap<String, Boolean> READY = new ConcurrentHashMap<String, Boolean>();

    private ModernSkins() {
    }

    public static String keyFor(AbstractClientPlayer player) {
        ResourceLocation location = player.getLocationSkin();
        return location == null ? null : location.toString();
    }

    static void markReady(String key, boolean slim) {
        READY.put(key, Boolean.valueOf(slim));
    }

    public static void forget(String key) {
        if(key != null) READY.remove(key);
    }

    public static Boolean slimFor(String key) {
        return key == null ? null : READY.get(key);
    }
}
