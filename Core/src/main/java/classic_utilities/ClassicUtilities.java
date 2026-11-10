package classic_utilities;

import classic_utilities.math.ZoomMath;
import classic_utilities.rendering.ChunkBorderMesh;
import classic_utilities.rendering.HudMessage;
import classic_utilities.world.SpawnScan;

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

    public static final float ZOOM_DEFAULT = 4.0F;
    public static final float ZOOM_MIN = 1.25F;
    public static final float ZOOM_MAX = 40.0F;
    public static final float ZOOM_STEP = 1.15F;
    public static final int ZOOM_HUD_HOLD_MS = 700;
    public static final int ZOOM_HUD_FADE_MS = 400;

    private boolean freecam;
    private boolean fullbright;
    private boolean lightOverlay;
    private boolean chunkBorders;
    private boolean zoomHeld;
    private boolean zoomApplied;
    private float zoomFactor = ZOOM_DEFAULT;
    private int overlayTimer;
    private float savedGamma = 1.0F;
    private boolean gammaChecked;

    protected final SpawnScan spawnScan = new SpawnScan();
    protected final ChunkBorderMesh chunkBorderMesh = new ChunkBorderMesh();
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

    protected abstract void zoomApply(float factor);

    protected abstract void zoomRestore();

    protected abstract int zoomConsumeScroll(boolean zoomActive);

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

    public boolean isChunkBorders() {
        return chunkBorders;
    }

    public boolean isZooming() {
        return zoomHeld;
    }

    public float getZoomFactor() {
        return zoomFactor;
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

    public void toggleChunkBorders() {
        chunkBorders = !chunkBorders;
        showMessage("Chunk Borders: " + (chunkBorders ? "ON" : "OFF"));
    }

    public void setZoomHeld(boolean held) {
        zoomHeld = held;
    }

    public void zoomRenderStart() {
        if(!zoomHeld) return;
        zoomApplied = true;
        zoomApply(zoomFactor);
    }

    public void zoomRenderEnd() {
        if(!zoomApplied) return;
        zoomApplied = false;
        zoomRestore();
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

        int notches = zoomConsumeScroll(zoomHeld);
        if(notches != 0) {
            zoomFactor = ZoomMath.step(zoomFactor, notches, ZOOM_STEP, ZOOM_MIN, ZOOM_MAX);
            showMessage("Zoom: " + ZoomMath.format(zoomFactor) + "x", ZOOM_HUD_HOLD_MS, ZOOM_HUD_FADE_MS);
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