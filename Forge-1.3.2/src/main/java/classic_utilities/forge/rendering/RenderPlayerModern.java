package classic_utilities.forge.rendering;

import classic_utilities.forge.skin.ModelPlayerModern;
import classic_utilities.forge.skin.ModernSkins;
import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelBiped;
import net.minecraft.src.RenderPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.src.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class RenderPlayerModern extends RenderPlayer {
    private final ModelBase vanillaModel;
    private final ModelPlayerModern classicModel;
    private final ModelPlayerModern slimModel;

    public RenderPlayerModern() {
        super();
        this.vanillaModel = this.mainModel;
        this.classicModel = new ModelPlayerModern((ModelBiped) vanillaModel, false);
        this.slimModel = new ModelPlayerModern((ModelBiped) vanillaModel, true);
    }

    @Override
    public void renderPlayer(EntityPlayer player, double x, double y, double z, float yaw, float partialTicks) {
        this.mainModel = pickModel(player);
        super.renderPlayer(player, x, y, z, yaw, partialTicks);
    }

    @Override
    public void drawFirstPersonHand() {
        // This version passes no player, but the first person hand is always the local player's.
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ModelBase picked = player == null ? vanillaModel : pickModel(player);
        if(!(picked instanceof ModelPlayerModern)) {
            super.drawFirstPersonHand();
            return;
        }

        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        ModelPlayerModern model = (ModelPlayerModern) picked;
        model.onGround = 0.0F;
        model.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
        model.bipedRightArm.render(0.0625F);
        model.renderSleeve(0.0625F);
    }

    private ModelBase pickModel(EntityPlayer player) {
        Boolean slim = ModernSkins.slimFor(player.skinUrl);
        if(slim == null) return vanillaModel;
        return slim.booleanValue() ? slimModel : classicModel;
    }
}