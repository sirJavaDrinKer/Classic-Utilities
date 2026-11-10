package classic_utilities.forge.rendering;

import classic_utilities.rendering.LightOverlayRenderer;
import classic_utilities.world.SpawnScan;
import net.minecraft.client.Minecraft;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.Tessellator;
import org.lwjgl.opengl.GL11;

public class ForgeLightOverlayRenderer extends LightOverlayRenderer {
    private final Minecraft client;

    private double camX, camY, camZ;

    public ForgeLightOverlayRenderer(Minecraft client, SpawnScan spawnScan) {
        super(spawnScan);
        this.client = client;
    }

    @Override
    protected boolean prepare(float partialTicks) {
        EntityLiving view = client.renderViewEntity;
        if(view == null) return false;

        camX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        camY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        camZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;
        return true;
    }

    @Override
    protected double cameraX() {
        return camX;
    }

    @Override
    protected double cameraY() {
        return camY;
    }

    @Override
    protected double cameraZ() {
        return camZ;
    }

    @Override
    protected void begin(float r, float g, float b, float a) {
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
        t.setColorRGBA_F(r, g, b, a);
    }

    @Override
    protected void horizontalQuad(double x0, double z0, double x1, double z1, double y) {
        Tessellator t = Tessellator.instance;
        t.addVertex(x0, y, z0);
        t.addVertex(x0, y, z1);
        t.addVertex(x1, y, z1);
        t.addVertex(x1, y, z0);
    }

    @Override
    protected void end() {
        Tessellator.instance.draw();
        GL11.glPopAttrib();
    }
}
