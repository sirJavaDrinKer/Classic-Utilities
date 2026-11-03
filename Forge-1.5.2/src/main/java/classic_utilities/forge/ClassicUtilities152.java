package classic_utilities.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import classic_utilities.EnchantPreview;
import classic_utilities.FreecamMath;
import classic_utilities.ClassicUtilities;
import classic_utilities.SpawnScan;
import classic_utilities.ZoomMath;

import java.util.ArrayList;
import java.util.List;


public abstract class ClassicUtilities152 extends ClassicUtilities {
    protected final Minecraft client = Minecraft.getMinecraft();

    private EntityFreecam camera;
    private EntityClientPlayerMP trackedPlayer;
    private MovementInput savedInput;
    private float frozenYaw, frozenPitch;

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
        EntityClientPlayerMP player = client.thePlayer;
        if(player == null || client.theWorld == null) return false;

        EntityFreecam cam = new EntityFreecam(client.theWorld);
        cam.setPosition(player.posX, player.posY, player.posZ);
        cam.prevPosX = cam.lastTickPosX = player.posX;
        cam.prevPosY = cam.lastTickPosY = player.posY;
        cam.prevPosZ = cam.lastTickPosZ = player.posZ;
        cam.rotationYaw = cam.prevRotationYaw = player.rotationYaw;
        cam.rotationPitch = cam.prevRotationPitch = player.rotationPitch;

        camera = cam;
        trackedPlayer = player;
        frozenYaw = player.rotationYaw;
        frozenPitch = player.rotationPitch;

        savedInput = player.movementInput;
        player.movementInput = new MovementInput();

        client.renderViewEntity = cam;
        return true;
    }

    @Override
    protected void freecamDisable() {
        if(trackedPlayer != null && savedInput != null) {
            trackedPlayer.movementInput = savedInput;
        }
        if(camera != null && client.renderViewEntity == camera && client.thePlayer != null) {
            client.renderViewEntity = client.thePlayer;
        }
        camera = null;
        trackedPlayer = null;
        savedInput = null;
    }

    @Override
    protected boolean freecamValid() {
        return camera != null
                && client.thePlayer != null
                && client.thePlayer == trackedPlayer
                && trackedPlayer.isEntityAlive()
                && client.theWorld != null
                && client.theWorld == camera.worldObj
                && client.renderViewEntity == camera;
    }

    @Override
    protected void freecamTickStart() {
        client.objectMouseOver = null;
    }

    @Override
    protected void freecamTickEnd() {
        if(camera == null) return;

        camera.prevPosX = camera.lastTickPosX = camera.posX;
        camera.prevPosY = camera.lastTickPosY = camera.posY;
        camera.prevPosZ = camera.lastTickPosZ = camera.posZ;

        if(client.currentScreen != null) return;

        GameSettings gs = client.gameSettings;
        double forward = 0, strafe = 0, vertical = 0;
        if(gs.keyBindForward.pressed) forward += 1;
        if(gs.keyBindBack.pressed) forward -= 1;
        if(gs.keyBindLeft.pressed) strafe += 1;
        if(gs.keyBindRight.pressed) strafe -= 1;
        if(gs.keyBindJump.pressed) vertical += 1;
        if(gs.keyBindSneak.pressed) vertical -= 1;

        double speed = FREECAM_SPEED * (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ? FREECAM_BOOST : 1.0D);
        double[] v = FreecamMath.velocity(camera.rotationYaw, camera.rotationPitch, forward, strafe, vertical, speed);

        camera.setPosition(camera.posX + v[0], camera.posY + v[1], camera.posZ + v[2]);
    }

    @Override
    protected void freecamRenderTick() {
        if(camera == null || trackedPlayer == null) return;

        float dYaw = trackedPlayer.rotationYaw - frozenYaw;
        float dPitch = trackedPlayer.rotationPitch - frozenPitch;

        if(dYaw != 0.0F || dPitch != 0.0F) {
            camera.rotationYaw += dYaw;
            float p = camera.rotationPitch + dPitch;
            if(p > 90.0F) p = 90.0F;
            if(p < -90.0F) p = -90.0F;
            camera.rotationPitch = p;

            trackedPlayer.rotationYaw = trackedPlayer.prevRotationYaw = frozenYaw;
            trackedPlayer.rotationPitch = trackedPlayer.prevRotationPitch = frozenPitch;
        }

        camera.prevRotationYaw = camera.rotationYaw;
        camera.prevRotationPitch = camera.rotationPitch;
    }

    private static final double OVERLAY_HEIGHT = 0.01D;
    private static final double OVERLAY_INSET = 0.03D;

    @Override
    protected void lightOverlayScan() {
        EntityLiving view = client.renderViewEntity;
        World world = client.theWorld;
        if(view == null || world == null) {
            spawnScan.clear();
            return;
        }

        int cx = MathHelper.floor_double(view.posX);
        int cy = MathHelper.floor_double(view.posY - view.yOffset); //feet
        int cz = MathHelper.floor_double(view.posZ);
        spawnScan.scan(new MobSpawnQuery(world), cx, cy, cz, OVERLAY_RADIUS, OVERLAY_VERTICAL_RADIUS);
    }


    private static class MobSpawnQuery implements SpawnScan.Query {
        private final World world;

        MobSpawnQuery(World world) {
            this.world = world;
        }

        @Override
        public boolean canSpawn(int x, int y, int z) {
            if(world.getBlockId(x, y - 1, z) == 0) return false;

            int light = OVERLAY_IGNORE_SKYLIGHT
                    ? world.getSavedLightValue(EnumSkyBlock.Block, x, y, z)
                    : world.getBlockLightValue(x, y, z);
            if(light > SPAWN_MAX_LIGHT) return false;


            return SpawnerAnimals.canCreatureTypeSpawnAtLocation(EnumCreatureType.monster, world, x, y, z);
        }
    }


    public void renderLightOverlay(float partialTicks) {
        if(!isLightOverlay()) return;

        int count = spawnScan.size();
        EntityLiving view = client.renderViewEntity;
        if(count == 0 || view == null) return;

        double vx = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double vy = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double vz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_POLYGON_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0F, -2.0F);

        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(1.0F, 0.0F, 0.0F, OVERLAY_ALPHA);
        for(int i = 0; i < count; i++) {
            double x0 = spawnScan.x(i) - vx + OVERLAY_INSET;
            double x1 = spawnScan.x(i) - vx + 1.0D - OVERLAY_INSET;
            double y = spawnScan.y(i) - vy + OVERLAY_HEIGHT;
            double z0 = spawnScan.z(i) - vz + OVERLAY_INSET;
            double z1 = spawnScan.z(i) - vz + 1.0D - OVERLAY_INSET;

            t.addVertex(x0, y, z0);
            t.addVertex(x0, y, z1);
            t.addVertex(x1, y, z1);
            t.addVertex(x1, y, z0);
        }
        t.draw();

        GL11.glPopAttrib();
    }

    private static final int HUD_BOTTOM_OFFSET = 72;

    public void renderHudMessage() {
        String text = hudMessage.getText();
        float opacity = hudMessage.getOpacity();
        if(text == null || opacity <= 0.0F) return;
        if(client.currentScreen != null || client.gameSettings.hideGUI) return;

        int alpha = (int) (opacity * 255.0F);
        if(alpha < 4) return;

        client.entityRenderer.setupOverlayRendering();
        ScaledResolution res = new ScaledResolution(client.gameSettings, client.displayWidth, client.displayHeight);
        FontRenderer font = client.fontRenderer;

        int x = (res.getScaledWidth() - font.getStringWidth(text)) / 2;
        int y = res.getScaledHeight() - HUD_BOTTOM_OFFSET;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        font.drawStringWithShadow(text, x, y, (alpha << 24) | 0xFFFFFF);
        GL11.glPopAttrib();
    }

    private boolean zoomOverridden;
    private float zoomSavedFov;
    private float zoomSavedSensitivity;
    private int lastHotbarSlot;

    @Override
    protected void zoomApply(float factor) {
        GameSettings gs = client.gameSettings;

        if(!zoomOverridden) {
            zoomSavedFov = gs.fovSetting;
            zoomSavedSensitivity = gs.mouseSensitivity;
            zoomOverridden = true;
        }

        gs.fovSetting = ZoomMath.fovSetting(zoomSavedFov, factor);
        gs.mouseSensitivity = ZoomMath.mouseSensitivity(zoomSavedSensitivity, factor);
    }

    @Override
    protected void zoomRestore() {
        if(!zoomOverridden) return;
        zoomOverridden = false;

        GameSettings gs = client.gameSettings;
        gs.fovSetting = zoomSavedFov;
        gs.mouseSensitivity = zoomSavedSensitivity;
    }

    @Override
    protected int zoomConsumeScroll(boolean zoomActive) {
        int wheel = Mouse.getDWheel();

        EntityClientPlayerMP player = client.thePlayer;
        if(player == null) return 0;

        if(zoomActive && wheel != 0 && client.currentScreen == null) {
            player.inventory.currentItem = lastHotbarSlot;

            int notches = Math.max(1, Math.abs(wheel) / 120);
            return wheel > 0 ? notches : -notches;
        }

        lastHotbarSlot = player.inventory.currentItem;
        return 0;
    }

    public void renderChunkBorders(float partialTicks) {
        if(!isChunkBorders()) return;

        EntityLiving view = client.renderViewEntity;
        if(view == null) return;

        double camX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double camY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double camZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        chunkBorderMesh.ensure(MathHelper.floor_double(view.posX) >> 4, MathHelper.floor_double(view.posZ) >> 4);

        Tessellator t = Tessellator.instance;
        t.setTranslation(-camX, -camY, -camZ);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);

            drawLines(t, chunkBorderMesh.corners(), 1.0F, 0.0F, 0.0F, 0.5F, 1.0F);
            drawLines(t, chunkBorderMesh.grid(), 1.0F, 1.0F, 0.0F, 1.0F, 1.0F);
            drawLines(t, chunkBorderMesh.sections(), 0.25F, 0.25F, 1.0F, 1.0F, 2.0F);
        } finally {
            GL11.glPopAttrib();
            t.setTranslation(0.0D, 0.0D, 0.0D);
        }
    }

    private static void drawLines(Tessellator t, double[] xyz, float r, float g, float b, float a, float width) {
        GL11.glLineWidth(width);
        t.startDrawing(GL11.GL_LINES);
        t.setColorRGBA_F(r, g, b, a);
        for(int i = 0; i < xyz.length; i += 3) {
            t.addVertex(xyz[i], xyz[i + 1], xyz[i + 2]);
        }
        t.draw();
    }

    private static final int ENCHANT_GUI_WIDTH = 176;
    private static final int ENCHANT_GUI_HEIGHT = 166;
    private static final int PREVIEW_MARGIN = 6;
    private static final int PREVIEW_PADDING = 4;
    private static final int PREVIEW_LINE_HEIGHT = 10;

    public void renderEnchantPreview() {
        EntityClientPlayerMP player = client.thePlayer;
        GuiScreen screen = client.currentScreen;
        if(player == null || screen == null) return;
        if(!(player.openContainer instanceof ContainerEnchantment)) return;

        ContainerEnchantment table = (ContainerEnchantment) player.openContainer;
        EnchantPreview preview = ClientEnchantState.get();
        if(!previewMatches(preview, table)) return;

        List<String> text = new ArrayList<String>();
        List<Integer> color = new ArrayList<Integer>();

        text.add("Enchant Preview");
        color.add(Integer.valueOf(0xFFAA00));

        for(int slot = 0; slot < EnchantPreview.SLOTS; slot++) {
            int cost = preview.costs[slot];
            if(cost <= 0) continue;

            boolean affordable = player.capabilities.isCreativeMode || player.experienceLevel >= cost;
            text.add("Slot " + (slot + 1) + " (level " + cost + ")");
            color.add(Integer.valueOf(affordable ? 0x55FF55 : 0xFF5555));

            int[] ids = preview.ids[slot];
            if(ids.length == 0) {
                text.add("  nothing");
                color.add(Integer.valueOf(0xAAAAAA));
            }
            for(int i = 0; i < ids.length; i++) {
                text.add("  " + enchantmentName(ids[i], preview.strengths[slot][i]));
                color.add(Integer.valueOf(0xFFFFFF));
            }
        }

        if(text.size() == 1) return;

        FontRenderer font = client.fontRenderer;
        int widest = 0;
        for(String line : text) {
            widest = Math.max(widest, font.getStringWidth(line));
        }

        int panelWidth = widest + PREVIEW_PADDING * 2;
        int panelHeight = text.size() * PREVIEW_LINE_HEIGHT + PREVIEW_PADDING * 2 - 2;

        int guiLeft = (screen.width - ENCHANT_GUI_WIDTH) / 2;
        int guiTop = (screen.height - ENCHANT_GUI_HEIGHT) / 2;
        int x = guiLeft + ENCHANT_GUI_WIDTH + PREVIEW_MARGIN;
        if(x + panelWidth > screen.width - 2) x = guiLeft - PREVIEW_MARGIN - panelWidth;
        if(x < 2) x = 2;

        client.entityRenderer.setupOverlayRendering();

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            fillRect(x, guiTop, x + panelWidth, guiTop + panelHeight, 0.0F, 0.0F, 0.0F, 0.65F);

            for(int i = 0; i < text.size(); i++) {
                font.drawStringWithShadow(text.get(i), x + PREVIEW_PADDING,
                        guiTop + PREVIEW_PADDING + i * PREVIEW_LINE_HEIGHT, color.get(i).intValue());
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static boolean previewMatches(EnchantPreview preview, ContainerEnchantment table) {
        if(preview == null || preview.windowId != table.windowId) return false;

        for(int i = 0; i < EnchantPreview.SLOTS; i++) {
            if(preview.costs[i] != table.enchantLevels[i]) return false;
        }
        return true;
    }

    private static String enchantmentName(int id, int strength) {
        Enchantment enchantment = id >= 0 && id < Enchantment.enchantmentsList.length ? Enchantment.enchantmentsList[id] : null;
        return enchantment != null ? enchantment.getTranslatedName(strength) : "Unknown (" + id + ") " + strength;
    }

    private static void fillRect(int x0, int y0, int x1, int y1, float r, float g, float b, float a) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(r, g, b, a);
        t.addVertex(x0, y1, 0.0D);
        t.addVertex(x1, y1, 0.0D);
        t.addVertex(x1, y0, 0.0D);
        t.addVertex(x0, y0, 0.0D);
        t.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}