package classic_utilities.forge.rendering;

import classic_utilities.forge.entity.EntityFreecam;
import classic_utilities.rendering.HitboxRenderer;
import classic_utilities.rendering.HitboxTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class ForgeHitboxRenderer extends HitboxRenderer {
    private static final double MAX_DISTANCE_SQ = MAX_DISTANCE * MAX_DISTANCE;
    private static final double DEG_TO_RAD = Math.PI / 180.0D;

    private static final double PLAYER_EYE_HEIGHT = 1.62D;

    private final Minecraft client;

    private double camX, camY, camZ;
    private Entity view;
    private boolean thirdPerson;
    private List entities;
    private int count;

    public ForgeHitboxRenderer(Minecraft client) {
        this.client = client;
    }

    @Override
    protected boolean prepare(float partialTicks) {
        EntityLiving v = client.renderViewEntity;
        if(v == null || client.theWorld == null) return false;

        view = v;
        camX = v.lastTickPosX + (v.posX - v.lastTickPosX) * partialTicks;
        camY = v.lastTickPosY + (v.posY - v.lastTickPosY) * partialTicks;
        camZ = v.lastTickPosZ + (v.posZ - v.lastTickPosZ) * partialTicks;
        thirdPerson = client.gameSettings.thirdPersonView != 0;

        entities = client.theWorld.loadedEntityList;
        count = entities.size();
        return true;
    }

    @Override
    protected int entityCount() {
        return count;
    }

    @Override
    protected boolean fill(int index, float partialTicks, HitboxTarget out) {
        if(index >= entities.size()) return false;

        Object o = entities.get(index);
        if(!(o instanceof Entity)) return false;

        Entity e = (Entity) o;
        if(e.isDead || e instanceof EntityFreecam) return false;

        if(e == view && !thirdPerson) return false;

        AxisAlignedBB bb = e.boundingBox;
        if(bb == null) return false;

        double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks;
        double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks;
        double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks;

        double cx = x - camX;
        double cy = y - camY;
        double cz = z - camZ;
        if(cx * cx + cy * cy + cz * cz > MAX_DISTANCE_SQ) return false;

        double dx = x - e.posX;
        double dy = y - e.posY;
        double dz = z - e.posZ;
        out.minX = bb.minX + dx;
        out.minY = bb.minY + dy;
        out.minZ = bb.minZ + dz;
        out.maxX = bb.maxX + dx;
        out.maxY = bb.maxY + dy;
        out.maxZ = bb.maxZ + dz;

        boolean player = e instanceof EntityPlayer;
        boolean living = e instanceof EntityLiving;
        out.living = living;

        double eye;
        if(player) {
            eye = PLAYER_EYE_HEIGHT;
        } else if(living) {
            eye = ((EntityLiving) e).getEyeHeight();
        } else {
            eye = e.height * 0.85D;
        }
        out.eyeY = out.minY + eye;

        float yaw = e.rotationYaw;
        float prevYaw = e.prevRotationYaw;
        if(living && !player) {
            EntityLiving l = (EntityLiving) e;
            yaw = l.rotationYawHead;
            prevYaw = l.prevRotationYawHead;
        }
        double yawRad = (prevYaw + (yaw - prevYaw) * partialTicks) * DEG_TO_RAD;
        double pitchRad = (e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * partialTicks) * DEG_TO_RAD;
        double flat = Math.cos(pitchRad);
        out.lookX = -Math.sin(yawRad) * flat;
        out.lookY = -Math.sin(pitchRad);
        out.lookZ = Math.cos(yawRad) * flat;
        return true;
    }

    @Override
    protected void begin() {
        Tessellator.instance.setTranslation(-camX, -camY, -camZ);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
    }

    @Override
    protected void drawLines(double[] xyz, float r, float g, float b, float a, float width) {
        RenderingUtil.drawLines(Tessellator.instance, xyz, r, g, b, a, width);
    }

    @Override
    protected void end() {
        GL11.glPopAttrib();
        Tessellator.instance.setTranslation(0.0D, 0.0D, 0.0D);
    }
}
