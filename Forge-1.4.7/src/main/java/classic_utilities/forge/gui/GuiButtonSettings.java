package classic_utilities.forge.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class GuiButtonSettings extends GuiButton {
    private static final String COG_TEXTURE = "/assets/classic_utilities/textures/gui/cog.png";
    private static final int ICON_SIZE = 18;
    private static final double UV_MAX = 15.0D / 16.0D;

    public GuiButtonSettings(int id, int x, int y) {
        super(id, x, y, 20, 20, "");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if(!this.drawButton) {
            return;
        }

        super.drawButton(mc, mouseX, mouseY);

        boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.renderEngine.getTexture(COG_TEXTURE));
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if(!this.enabled) {
            GL11.glColor4f(0.627F, 0.627F, 0.627F, 1.0F);
        } else if(hovered) {
            GL11.glColor4f(0.9F, 0.9F, 1.0F, 1.0F);
        } else {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }

        int x = this.xPosition + (this.width - ICON_SIZE) / 2;
        int y = this.yPosition + (this.height - ICON_SIZE) / 2;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x, y + ICON_SIZE, this.zLevel, 0.0D, UV_MAX);
        t.addVertexWithUV(x + ICON_SIZE, y + ICON_SIZE, this.zLevel, UV_MAX, UV_MAX);
        t.addVertexWithUV(x + ICON_SIZE, y, this.zLevel, UV_MAX, 0.0D);
        t.addVertexWithUV(x, y, this.zLevel, 0.0D, 0.0D);
        t.draw();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
    }
}