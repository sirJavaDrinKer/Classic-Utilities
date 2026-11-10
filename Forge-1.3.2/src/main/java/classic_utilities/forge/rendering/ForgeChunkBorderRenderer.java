package classic_utilities.forge.rendering;

import classic_utilities.rendering.ChunkBorderMesh;
import classic_utilities.rendering.ChunkBorderRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.MathHelper;
import net.minecraft.src.Tessellator;
import org.lwjgl.opengl.GL11;

public class ForgeChunkBorderRenderer extends ChunkBorderRenderer {
    private final Minecraft client;

    private double camX, camY, camZ;
    private int blockX, blockZ;

    public ForgeChunkBorderRenderer(Minecraft client, ChunkBorderMesh chunkBorderMesh) {
        super(chunkBorderMesh);
        this.client = client;
    }

    @Override
    protected boolean prepare(float partialTicks) {
        EntityLiving view = client.renderViewEntity;
        if(view == null) return false;

        camX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        camY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        camZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;
        blockX = MathHelper.floor_double(view.posX);
        blockZ = MathHelper.floor_double(view.posZ);
        return true;
    }

    @Override
    protected int cameraBlockX() {
        return blockX;
    }

    @Override
    protected int cameraBlockZ() {
        return blockZ;
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
