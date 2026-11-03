package classic_utilities;

public final class ZoomMath {
    public static final float BASE_FOV = 70.0F;
    public static final float FOV_PER_SETTING = 40.0F;

    public static final float MIN_FOV = 2.0F;

    public static final float MIN_SENSITIVITY_BASE = 0.05F;

    private ZoomMath() {
    }

    public static float fovSetting(float baseFovSetting, float zoomFactor) {
        if(zoomFactor < 1.0F) zoomFactor = 1.0F;

        float baseFov = BASE_FOV + baseFovSetting * FOV_PER_SETTING;
        float fov = Math.max(MIN_FOV, baseFov / zoomFactor);
        return (fov - BASE_FOV) / FOV_PER_SETTING;
    }

    public static float mouseSensitivity(float baseSensitivity, float zoomFactor) {
        if(zoomFactor < 1.0F) zoomFactor = 1.0F;

        float base = baseSensitivity * 0.6F + 0.2F;
        float scaled = Math.max(MIN_SENSITIVITY_BASE, base / (float) Math.cbrt(zoomFactor));
        return (scaled - 0.2F) / 0.6F;
    }

    public static float step(float zoomFactor, int notches, float stepMultiplier, float min, float max) {
        float next = zoomFactor * (float) Math.pow(stepMultiplier, notches);
        if(next < min) return min;
        if(next > max) return max;
        return next;
    }

    public static String format(float zoomFactor) {
        int tenths = Math.round(zoomFactor * 10.0F);
        return (tenths / 10) + "." + (tenths % 10);
    }
}
