package classic_utilities.options;

public final class WorldOptions {
    public static final int FREECAM = 0;
    public static final int ENCHANT_PREVIEW = 1;
    public static final int ENDERMAN_GRIEFING = 2;
    public static final int FREECAM_COLLISION = 3;
    public static final int NOFOG = 4;
    public static final int REMOVE_ANVIL_LIMIT = 5;
    public static final int COUNT = 6;

    public static final WorldOptions DEFAULTS = new WorldOptions(true, true, true, false, true, false);

    public final boolean allowFreecam;
    public final boolean useEnchantPreview;
    public final boolean allowEndermanGriefing;
    public final boolean freecamCollision;
    public final boolean allowNoFog;
    public final boolean removeAnvilLimit;

    public WorldOptions(boolean allowFreecam, boolean useEnchantPreview, boolean allowEndermanGriefing, boolean freecamCollision, boolean allowNoFog, boolean removeAnvilLimit) {
        this.allowFreecam = allowFreecam;
        this.useEnchantPreview = useEnchantPreview;
        this.allowEndermanGriefing = allowEndermanGriefing;
        this.freecamCollision = freecamCollision;
        this.allowNoFog = allowNoFog;
        this.removeAnvilLimit = removeAnvilLimit;
    }

    public boolean get(int id) {
        switch(id) {
            case FREECAM: return allowFreecam;
            case ENCHANT_PREVIEW: return useEnchantPreview;
            case ENDERMAN_GRIEFING: return allowEndermanGriefing;
            case FREECAM_COLLISION: return freecamCollision;
            case NOFOG: return allowNoFog;
            case REMOVE_ANVIL_LIMIT: return removeAnvilLimit;
            default: throw new IllegalArgumentException("Unknown option id " + id);
        }
    }

    public WorldOptions with(int id, boolean value) {
        switch(id) {
            case FREECAM: return new WorldOptions(value, useEnchantPreview, allowEndermanGriefing, freecamCollision, allowNoFog, removeAnvilLimit);
            case ENCHANT_PREVIEW: return new WorldOptions(allowFreecam, value, allowEndermanGriefing, freecamCollision, allowNoFog, removeAnvilLimit);
            case ENDERMAN_GRIEFING: return new WorldOptions(allowFreecam, useEnchantPreview, value, freecamCollision, allowNoFog, removeAnvilLimit);
            case FREECAM_COLLISION: return new WorldOptions(allowFreecam, useEnchantPreview, allowEndermanGriefing, value, allowNoFog, removeAnvilLimit);
            case NOFOG: return new WorldOptions(allowFreecam, useEnchantPreview, allowEndermanGriefing, freecamCollision, value, removeAnvilLimit);
            case REMOVE_ANVIL_LIMIT: return new WorldOptions(allowFreecam, useEnchantPreview, allowEndermanGriefing, freecamCollision, allowNoFog, value);
            default: throw new IllegalArgumentException("Unknown option id " + id);
        }
    }

    public static boolean isValidId(int id) {
        return id >= 0 && id < COUNT;
    }

    public static String label(int id) {
        switch(id) {
            case FREECAM: return "Allow Freecam";
            case ENCHANT_PREVIEW: return "Use Enchant Preview";
            case ENDERMAN_GRIEFING: return "Allow Enderman Griefing";
            case FREECAM_COLLISION: return "Freecam Collisions";
            case NOFOG: return "Allow No Fog";
            case REMOVE_ANVIL_LIMIT: return "Disable Too Expensive";
            default: throw new IllegalArgumentException("Unknown option id " + id);
        }
    }

    public static String key(int id) {
        switch(id) {
            case FREECAM: return "allowFreecam";
            case ENCHANT_PREVIEW: return "useEnchantPreview";
            case ENDERMAN_GRIEFING: return "allowEndermanGriefing";
            case FREECAM_COLLISION: return "freecamCollision";
            case NOFOG: return "allowNoFog";
            case REMOVE_ANVIL_LIMIT: return "removeAnvilLimit";
            default: throw new IllegalArgumentException("Unknown option id " + id);
        }
    }
}
