package classic_utilities.forge;

import classic_utilities.EnchantPreview;

public final class ClientEnchantState {
    private static volatile EnchantPreview preview;

    private ClientEnchantState() {
    }

    public static EnchantPreview get() {
        return preview;
    }

    public static void set(EnchantPreview newPreview) {
        preview = newPreview;
    }
}
