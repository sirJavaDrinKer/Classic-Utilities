package classic_utilities.forge;

import classic_utilities.ClassicUtilities;
import classic_utilities.forge.feature.FreecamController;
import classic_utilities.forge.feature.LightOverlayScanner;
import classic_utilities.forge.feature.NoFogController;
import classic_utilities.forge.feature.SkinFixController;
import classic_utilities.forge.feature.VanillaHitboxes;
import classic_utilities.forge.feature.ZoomController;
import classic_utilities.forge.gui.GuiOptionsMenu;
import classic_utilities.options.ClientOptions;
import net.minecraft.client.Minecraft;

public abstract class ClassicUtilities132 extends ClassicUtilities {
    protected final Minecraft client = Minecraft.getMinecraft();

    private final FreecamController freecamController = new FreecamController(client);
    private final ZoomController zoomController = new ZoomController(client);
    private final LightOverlayScanner lightOverlayScanner = new LightOverlayScanner(client, spawnScan);
    private final NoFogController noFogController = new NoFogController(client);
    private final SkinFixController skinFixController = new SkinFixController(client);
    private final VanillaHitboxes vanillaHitboxes = new VanillaHitboxes();

    @Override
    protected float getGamma() {
        return client.gameSettings.gammaSetting;
    }

    @Override
    protected void setGamma(float gamma) {
        client.gameSettings.gammaSetting = gamma;
    }

    @Override
    protected void say(String message) {
        if(client.ingameGUI != null) {
            client.ingameGUI.getChatGUI().printChatMessage("\u00a7e[CUtil] \u00a7f" + message);
        }
    }

    @Override
    protected boolean freecamEnable() {
        return freecamController.enable();
    }

    @Override
    protected void freecamDisable() {
        freecamController.disable();
    }

    @Override
    protected boolean freecamValid() {
        return freecamController.isValid();
    }

    @Override
    protected void freecamTickStart() {
        freecamController.tickStart();
    }

    @Override
    protected void freecamTickEnd() {
        freecamController.tickEnd();
    }

    @Override
    protected void freecamRenderTick() {
        freecamController.renderTick();
    }

    @Override
    protected void lightOverlayScan() {
        lightOverlayScanner.scan();
    }

    @Override
    protected void zoomApply(float factor) {
        zoomController.apply(factor);
    }

    @Override
    protected void zoomRestore() {
        zoomController.restore();
    }

    @Override
    protected int zoomConsumeScroll(boolean zoomActive) {
        return zoomController.consumeScroll(zoomActive);
    }

    @Override
    protected boolean noFogSupported() {
        return noFogController.isSupported();
    }

    @Override
    protected void noFogApply(boolean enabled) {
        noFogController.apply(enabled);
    }

    @Override
    protected void hitboxRenderTick() {
        if(ClientOptions.isBetterHitboxes()) vanillaHitboxes.suppress();
    }

    @Override
    protected void skinFixTick() {
        skinFixController.tick();
    }

    protected void openMenu() {
        client.displayGuiScreen(new GuiOptionsMenu(client.currentScreen));
    }
}
