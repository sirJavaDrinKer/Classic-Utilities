package classic_utilities.forge.rendering;

import classic_utilities.rendering.HudMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.src.FontRenderer;
import net.minecraft.src.ScaledResolution;
import org.lwjgl.opengl.GL11;

public class MessageRenderer {
    private static final int HUD_BOTTOM_OFFSET = 72;
    public static void renderMessage(Minecraft client, HudMessage hudMessage) {
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
