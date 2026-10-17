package classic_utilities;

public abstract class ClassicUtilities {
    public static final String MODID = "classic_utilities";
    public static final String NAME = "Classic Utilities";

    public static ClassicUtilities instance;

    public static final double FREECAM_SPEED = 0.6D;
    public static final double FREECAM_BOOST = 3.0D;

    public static final float FULLBRIGHT_GAMMA = 16.0F;

    public static final int OVERLAY_RADIUS = 16;
    public static final int OVERLAY_VERTICAL_RADIUS = 8;
    public static final int OVERLAY_REFRESH_TICKS = 5;
    public static final int SPAWN_MAX_LIGHT = 7;

    public static final boolean OVERLAY_IGNORE_SKYLIGHT = true;
    public static final float OVERLAY_ALPHA = 0.4F;

    public static final int HUD_HOLD_MS = 2000;
    public static final int HUD_FADE_MS = 1000;

    private boolean freecam;
    private boolean fullbright;
    private boolean lightOverlay;
    private int overlayTimer;
    private float savedGamma = 1.0F;
    private boolean gammaChecked;

    protected final SpawnScan spawnScan = new SpawnScan();
    protected final HudMessage hudMessage = new HudMessage();

    public ClassicUtilities() {
        if(instance == null) {
            instance = this;
        } else {
            throw new IllegalStateException(NAME + " instantiated twice!");
        }
    }

    protected abstract float getGamma();
    protected abstract void setGamma(float gamma);
    protected abstract void say(String message);

    protected abstract boolean freecamEnable();
    protected abstract void freecamDisable();
    protected abstract boolean freecamValid();
    protected abstract void freecamTickStart();
    protected abstract void freecamTickEnd();
    protected abstract void freecamRenderTick();


    protected abstract void lightOverlayScan();

    public void showMessage(String text) {
        showMessage(text, HUD_HOLD_MS, HUD_FADE_MS);
    }

    public void showMessage(String text, int holdMs, int fadeMs) {
        hudMessage.show(text, holdMs, fadeMs);
    }

    public boolean isFreecam() {
        return freecam;
    }

    public boolean isFullbright() {
        return fullbright;
    }

    public boolean isLightOverlay() {
        return lightOverlay;
    }

    public void toggleFreecam() {
        if(freecam) {
            freecam = false;
            freecamDisable();
            showMessage("Freecam: OFF");
        } else if(freecamEnable()) {
            freecam = true;
            showMessage("Freecam: ON");
        }
    }

    public void toggleFullbright() {
        fullbright = !fullbright;
        if(fullbright) {
            savedGamma = getGamma();
            setGamma(FULLBRIGHT_GAMMA);
        } else {
            setGamma(savedGamma);
        }
        showMessage("Fullbright: " + (fullbright ? "ON" : "OFF"));
    }

    public void toggleLightOverlay() {
        lightOverlay = !lightOverlay;
        if(lightOverlay) {
            overlayTimer = OVERLAY_REFRESH_TICKS;
            lightOverlayScan();
        } else {
            spawnScan.clear();
        }
        showMessage("Light Overlay: " + (lightOverlay ? "ON" : "OFF"));
    }

    public void clientTickStart() {
        if(!gammaChecked) {
            gammaChecked = true;
            if(getGamma() > 1.0F) setGamma(1.0F);
        }

        if(fullbright) maintainFullbright();

        if(freecam) {
            if(!freecamValid()) {
                freecam = false;
                freecamDisable();
                showMessage("Freecam: OFF");
                return;
            }
            freecamTickStart();
        }
    }

    public void clientTickEnd() {
        if(freecam) freecamTickEnd();

        if(lightOverlay && --overlayTimer <= 0) {
            overlayTimer = OVERLAY_REFRESH_TICKS;
            lightOverlayScan();
        }
    }

    public void renderTick() {
        if(fullbright) maintainFullbright();
        if(freecam) freecamRenderTick();
    }

    private void maintainFullbright() {
        float g = getGamma();
        if(g != FULLBRIGHT_GAMMA) {
            savedGamma = g;
            setGamma(FULLBRIGHT_GAMMA);
        }
    }
}