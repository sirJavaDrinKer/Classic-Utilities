package classic_utilities.forge.rendering;

import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

public class RenderingUtil {
    public static void drawLines(Tessellator t, double[] xyz, float r, float g, float b, float a, float width) {
        GL11.glLineWidth(width);
        t.startDrawing(GL11.GL_LINES);
        t.setColorRGBA_F(r, g, b, a);
        for(int i = 0; i < xyz.length; i += 3) {
            t.addVertex(xyz[i], xyz[i + 1], xyz[i + 2]);
        }
        t.draw();
    }
    public static void fillRect(int x0, int y0, int x1, int y1, float r, float g, float b, float a) {
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
