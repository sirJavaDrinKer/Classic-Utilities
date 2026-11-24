package classic_utilities.forge;

import classic_utilities.input.DebugCombo;
import classic_utilities.forge.rendering.ForgeEnchantPreviewRenderer;
import classic_utilities.forge.rendering.ForgeChunkBorderRenderer;
import classic_utilities.forge.rendering.ForgeHitboxRenderer;
import classic_utilities.forge.rendering.ForgeLightOverlayRenderer;
import classic_utilities.forge.rendering.MessageRenderer;
import classic_utilities.forge.rendering.WorldRenderHook;
import classic_utilities.forge.gui.GuiOptionsScreenHook;
import classic_utilities.forge.options.OptionsClient;
import classic_utilities.options.ClientOptions;
import classic_utilities.options.ClientWorldOptions;
import net.minecraft.world.World;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

import java.util.EnumSet;

public class ForgeImpl extends ClassicUtilities147 {
    private final KeyBinding FREECAM_KEY = new KeyBinding("classic_utilities.freecam", Keyboard.KEY_F);
    private final KeyBinding FULLBRIGHT_KEY = new KeyBinding("classic_utilities.fullbright", Keyboard.KEY_V);
    private final KeyBinding LIGHT_OVERLAY_KEY = new KeyBinding("classic_utilities.lightoverlay", Keyboard.KEY_L);
    private final KeyBinding ZOOM_KEY = new KeyBinding("classic_utilities.zoom", Keyboard.KEY_C);
    private final KeyBinding NOFOG_KEY = new KeyBinding("classic_utilities.nofog", Keyboard.KEY_N);
    private final KeyBinding MENU_KEY = new KeyBinding("classic_utilities.menu", Keyboard.KEY_NONE);

    private final ForgeEnchantPreviewRenderer forgeEnchantPreviewRenderer = new ForgeEnchantPreviewRenderer(client);

    private final DebugCombo chunkBorderCombo = new DebugCombo();
    private final DebugCombo hitboxCombo = new DebugCombo();

    private static final int OPTIONS_QUERY_DELAY_TICKS = 10;

    private World lastWorld;
    private int optionsQueryCountdown = -1;

    private boolean freecamKeyWasDown;
    private boolean fullbrightKeyWasDown;
    private boolean lightOverlayKeyWasDown;
    private boolean noFogKeyWasDown;
    private boolean menuKeyWasDown;

    public void init() {
        LanguageRegistry.instance().loadLocalization("/assets/classic_utilities/lang/en_US.lang", "en_US", false);

        TickRegistry.registerTickHandler(new Ticker(), Side.CLIENT);

        MinecraftForge.EVENT_BUS.register(new WorldRenderHook(this,
                new ForgeLightOverlayRenderer(client, spawnScan),
                new ForgeChunkBorderRenderer(client, chunkBorderMesh),
                new ForgeHitboxRenderer(client)));

        KeyBindingRegistry.registerKeyBinding(new KeyBindingRegistry.KeyHandler(new KeyBinding[] { FREECAM_KEY, FULLBRIGHT_KEY, LIGHT_OVERLAY_KEY, ZOOM_KEY, NOFOG_KEY, MENU_KEY }) {
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
        boolean noFogDown = NOFOG_KEY.pressed;
        boolean menuDown = MENU_KEY.pressed;

        if(client.currentScreen == null && client.thePlayer != null && client.theWorld != null) {
            if(freecamDown && !freecamKeyWasDown) toggleFreecam();
            if(fullbrightDown && !fullbrightKeyWasDown) toggleFullbright();
            if(lightOverlayDown && !lightOverlayKeyWasDown) toggleLightOverlay();
            if(noFogDown && !noFogKeyWasDown) toggleNoFog();
            if (menuDown && !menuKeyWasDown) openMenu();
        }

        boolean inGame = client.currentScreen == null && client.thePlayer != null && client.theWorld != null;

        setZoomHeld(inGame && ZOOM_KEY.pressed);

        boolean f3Down = inGame && Keyboard.isKeyDown(Keyboard.KEY_F3);
        boolean gDown = inGame && Keyboard.isKeyDown(Keyboard.KEY_G);
        if(chunkBorderCombo.pollFrame(f3Down, gDown)) toggleChunkBorders();

        // F3+B is vanilla's own hitbox key. With Better Hitboxes on we take it over, otherwise leave it alone.
        boolean bDown = inGame && ClientOptions.isBetterHitboxes() && Keyboard.isKeyDown(Keyboard.KEY_B);
        if(hitboxCombo.pollFrame(f3Down, bDown)) toggleHitboxes();

        freecamKeyWasDown = freecamDown;
        fullbrightKeyWasDown = fullbrightDown;
        lightOverlayKeyWasDown = lightOverlayDown;
        noFogKeyWasDown = noFogDown;
    }

    private void trackWorldOptions() {
        World world = client.theWorld;
        if(world != lastWorld) {
            lastWorld = world;
            ClientWorldOptions.reset();
            optionsQueryCountdown = world == null ? -1 : OPTIONS_QUERY_DELAY_TICKS;
        }

        if(optionsQueryCountdown >= 0 && client.thePlayer != null && --optionsQueryCountdown < 0) {
            OptionsClient.sendQuery();
        }
    }

    private void undoDebugScreenToggle() {
        GameSettings gs = client.gameSettings;
        boolean f3Down = Keyboard.isKeyDown(Keyboard.KEY_F3);
        boolean undoChunkBorders = chunkBorderCombo.pollTick(f3Down, gs.showDebugInfo);
        boolean undoHitboxes = hitboxCombo.pollTick(f3Down, gs.showDebugInfo);
        if(undoChunkBorders) {
            gs.showDebugInfo = chunkBorderCombo.debugToRestore();
        } else if(undoHitboxes) {
            gs.showDebugInfo = hitboxCombo.debugToRestore();
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
            if(types.contains(TickType.CLIENT)) {
                GuiOptionsScreenHook.update(client);
                trackWorldOptions();
                clientTickStart();
            }
        }

        @Override
        public void tickEnd(EnumSet<TickType> types, Object... args) {
            if(types.contains(TickType.RENDER)) {
                zoomRenderEnd();
                renderTick();
                MessageRenderer.renderMessage(client, hudMessage);
                forgeEnchantPreviewRenderer.render();
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