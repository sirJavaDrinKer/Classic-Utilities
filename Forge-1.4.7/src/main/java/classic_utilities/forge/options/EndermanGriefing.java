package classic_utilities.forge.options;

public final class EndermanGriefing {
    private static volatile boolean allowed = true;

    private EndermanGriefing() {
    }

    public static void set(boolean allow) {
        allowed = allow;
    }

    public static boolean isAllowed() {
        return allowed;
    }

    public static boolean allowPickup(boolean carriable) {
        return carriable && allowed;
    }
}
