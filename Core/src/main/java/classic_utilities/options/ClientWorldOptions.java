package classic_utilities.options;

public final class ClientWorldOptions {
    private static final class State {
        final boolean synced;
        final boolean canEdit;
        final WorldOptions options;

        State(boolean synced, boolean canEdit, WorldOptions options) {
            this.synced = synced;
            this.canEdit = canEdit;
            this.options = options;
        }
    }

    private static volatile State state = new State(false, false, WorldOptions.DEFAULTS);

    private ClientWorldOptions() {
    }

    public static WorldOptions get() {
        return state.options;
    }

    public static boolean isSynced() {
        return state.synced;
    }

    public static boolean canEdit() {
        return state.synced && state.canEdit;
    }

    public static boolean isFreecamAllowed() {
        return state.options.allowFreecam;
    }

    public static boolean isFreecamCollisionEnabled() {
        return state.options.freecamCollision;
    }

    public static boolean isNoFogAllowed() {
        return state.options.allowNoFog;
    }

    public static boolean isAnvilLimitRemoved() {
        return state.options.removeAnvilLimit;
    }

    public static boolean isEnchantPreviewEnabled() {
        return state.options.useEnchantPreview;
    }

    public static void update(boolean canEdit, WorldOptions options) {
        state = new State(true, canEdit, options);
    }

    public static void reset() {
        state = new State(false, false, WorldOptions.DEFAULTS);
    }
}
