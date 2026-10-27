package classic_utilities.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import classic_utilities.FreecamMath;
import classic_utilities.ClassicUtilities;
import classic_utilities.SpawnScan;


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
}