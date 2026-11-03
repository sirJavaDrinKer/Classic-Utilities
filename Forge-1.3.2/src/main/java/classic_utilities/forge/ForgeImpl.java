package classic_utilities.forge;

import classic_utilities.DebugCombo;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.common.Side;
import net.minecraft.src.GameSettings;
import net.minecraft.src.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

import java.util.EnumSet;

public class ForgeImpl extends ClassicUtilities132 {
    private final KeyBinding FREECAM_KEY = new KeyBinding("classic_utilities.freecam", Keyboard.KEY_F);
    private final KeyBinding FULLBRIGHT_KEY = new KeyBinding("classic_utilities.fullbright", Keyboard.KEY_V);
    private final KeyBinding LIGHT_OVERLAY_KEY = new KeyBinding("classic_utilities.lightoverlay", Keyboard.KEY_L);
    private final KeyBinding ZOOM_KEY = new KeyBinding("classic_utilities.zoom", Keyboard.KEY_C);

    private final DebugCombo chunkBorderCombo = new DebugCombo();

    private boolean freecamKeyWasDown;
    private boolean fullbrightKeyWasDown;
    private boolean lightOverlayKeyWasDown;

    public void init() {
        LanguageRegistry lang = LanguageRegistry.instance();
        lang.addStringLocalization("classic_utilities.freecam", "en_US", "Freecam");
        lang.addStringLocalization("classic_utilities.fullbright", "en_US", "Fullbright");
        lang.addStringLocalization("classic_utilities.lightoverlay", "en_US", "Light Overlay");
        lang.addStringLocalization("classic_utilities.zoom", "en_US", "Zoom");

        TickRegistry.registerTickHandler(new Ticker(), Side.CLIENT);

        MinecraftForge.EVENT_BUS.register(new LightOverlayHook(this));

        KeyBindingRegistry.registerKeyBinding(new KeyBindingRegistry.KeyHandler(new KeyBinding[] { FREECAM_KEY, FULLBRIGHT_KEY, LIGHT_OVERLAY_KEY, ZOOM_KEY }) {
            @Override
            public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat) {
                //Dont care
            }

            @Override
            public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) {
                //Dont care
            }

            @Override
            public EnumSet<TickType> ticks() {
                return EnumSet.noneOf(TickType.class);
            }

            @Override
            public String getLabel() {
                return "Classic Utility key handler thing";
            }
        });
    }

    private void pollKeys() {
        boolean freecamDown = FREECAM_KEY.pressed;
        boolean fullbrightDown = FULLBRIGHT_KEY.pressed;
        boolean lightOverlayDown = LIGHT_OVERLAY_KEY.pressed;

        if(client.currentScreen == null && client.thePlayer != null && client.theWorld != null) {
            if(freecamDown && !freecamKeyWasDown) toggleFreecam();
            if(fullbrightDown && !fullbrightKeyWasDown) toggleFullbright();
            if(lightOverlayDown && !lightOverlayKeyWasDown) toggleLightOverlay();
        }

        boolean inGame = client.currentScreen == null && client.thePlayer != null && client.theWorld != null;

        setZoomHeld(inGame && ZOOM_KEY.pressed);

        boolean f3Down = inGame && Keyboard.isKeyDown(Keyboard.KEY_F3);
        boolean gDown = inGame && Keyboard.isKeyDown(Keyboard.KEY_G);
        if(chunkBorderCombo.pollFrame(f3Down, gDown)) toggleChunkBorders();

        freecamKeyWasDown = freecamDown;
        fullbrightKeyWasDown = fullbrightDown;
        lightOverlayKeyWasDown = lightOverlayDown;
    }

    private void undoDebugScreenToggle() {
        GameSettings gs = client.gameSettings;
        if(chunkBorderCombo.pollTick(Keyboard.isKeyDown(Keyboard.KEY_F3), gs.showDebugInfo)) {
            gs.showDebugInfo = chunkBorderCombo.debugToRestore();
        }
    }

    private class Ticker implements ITickHandler {
        private final EnumSet<TickType> ticks = EnumSet.of(TickType.CLIENT, TickType.RENDER);

        @Override
        public void tickStart(EnumSet<TickType> types, Object... args) {
            if(types.contains(TickType.RENDER)) {
                pollKeys();
                renderTick();
                zoomRenderStart();
            }
            if(types.contains(TickType.CLIENT)) clientTickStart();
        }

        @Override
        public void tickEnd(EnumSet<TickType> types, Object... args) {
            if(types.contains(TickType.RENDER)) {
                zoomRenderEnd();
                renderTick();
                renderHudMessage();
                renderEnchantPreview();
            }
            if(types.contains(TickType.CLIENT)) clientTickEnd();
            if(types.contains(TickType.CLIENT)) undoDebugScreenToggle();
        }

        @Override
        public EnumSet<TickType> ticks() {
            return ticks;
        }

        @Override
        public String getLabel() {
            return "Classic Utilities client ticker";
        }
    }
}