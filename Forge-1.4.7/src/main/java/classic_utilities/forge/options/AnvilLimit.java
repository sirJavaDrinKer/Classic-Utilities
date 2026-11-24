package classic_utilities.forge.options;

import classic_utilities.options.ClientWorldOptions;

/**
 * Hooks for anvil "Too Expensive", ASM find >= 40 limit.
 */
public final class AnvilLimit {
    private static volatile boolean removed = false;

    private AnvilLimit() {
    }

    public static void set(boolean remove) {
        removed = remove;
    }

    public static boolean isRemoved() {
        return removed;
    }

    // Called from ContainerRepair (logical server)
    public static int serverLimit(int limit) {
        return removed ? Integer.MAX_VALUE : limit;
    }

    // Called from GuiRepair (client, display only). Never touched on a dedicated server.
    public static int clientLimit(int limit) {
        return ClientWorldOptions.isAnvilLimitRemoved() ? Integer.MAX_VALUE : limit;
    }
}
