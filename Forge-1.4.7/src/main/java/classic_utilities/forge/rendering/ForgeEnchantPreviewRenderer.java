package classic_utilities.forge.rendering;

import classic_utilities.rendering.EnchantRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.inventory.ContainerEnchantment;
import org.lwjgl.opengl.GL11;


public class ForgeEnchantPreviewRenderer extends EnchantRenderer {
    private final Minecraft client;

    public ForgeEnchantPreviewRenderer(Minecraft client) {
        this.client = client;
    }

    private ContainerEnchantment table() {
        return (ContainerEnchantment) client.thePlayer.openContainer;
    }

    @Override
    protected boolean tableOpen() {
        EntityClientPlayerMP player = client.thePlayer;
        if(player == null || client.currentScreen == null) return false;
        return player.openContainer instanceof ContainerEnchantment;
    }

    @Override
    protected int tableWindowId() {
        return table().windowId;
    }

    @Override
    protected int tableCost(int slot) {
        return table().enchantLevels[slot];
    }

    @Override
    protected boolean canAfford(int cost) {
        EntityClientPlayerMP player = client.thePlayer;
        return player.capabilities.isCreativeMode || player.experienceLevel >= cost;
    }

    @Override
    protected String enchantmentName(int id, int strength) {
        Enchantment enchantment = id >= 0 && id < Enchantment.enchantmentsList.length ? Enchantment.enchantmentsList[id] : null;
        return enchantment != null ? enchantment.getTranslatedName(strength) : "Unknown (" + id + ") " + strength;
    }

    @Override
    protected int textWidth(String text) {
        return client.fontRenderer.getStringWidth(text);
    }

    @Override
    protected int screenWidth() {
        return client.currentScreen.width;
    }

    @Override
    protected int screenHeight() {
        return client.currentScreen.height;
    }

    @Override
    protected void begin() {
        client.entityRenderer.setupOverlayRendering();

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    protected void fillRect(int x0, int y0, int x1, int y1, float r, float g, float b, float a) {
        RenderingUtil.fillRect(x0, y0, x1, y1, r, g, b, a);
    }

    @Override
    protected void drawText(String text, int x, int y, int color) {
        client.fontRenderer.drawStringWithShadow(text, x, y, color);
    }

    @Override
    protected void end() {
        GL11.glPopAttrib();
    }
}
