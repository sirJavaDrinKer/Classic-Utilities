package classic_utilities.forge.rendering;

import classic_utilities.forge.skin.ModelPlayerModern;
import classic_utilities.forge.skin.ModernSkins;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
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
    public void func_82441_a(EntityPlayer player) {
        ModelBase picked = pickModel(player);
        if(!(picked instanceof ModelPlayerModern)) {
            super.func_82441_a(player);
            return;
        }

        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        ModelPlayerModern model = (ModelPlayerModern) picked;
        model.onGround = 0.0F;
        model.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);
        model.bipedRightArm.render(0.0625F);
        model.renderSleeve(0.0625F);
    }

    private ModelBase pickModel(EntityPlayer player) {
        Boolean slim = ModernSkins.slimFor(player.skinUrl);
        if(slim == null) return vanillaModel;
        return slim.booleanValue() ? slimModel : classicModel;
    }
}