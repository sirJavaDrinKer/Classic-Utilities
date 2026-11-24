package classic_utilities.forge.rendering;

import classic_utilities.forge.skin.ModelPlayerModern;
import classic_utilities.forge.skin.ModernSkins;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
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
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        this.mainModel = pickModel(entity);
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    public void renderFirstPersonArm(EntityPlayer player) {
        ModelBase picked = pickModel(player);
        if(!(picked instanceof ModelPlayerModern)) {
            super.renderFirstPersonArm(player);
            return;
        }

        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        ModelPlayerModern model = (ModelPlayerModern) picked;
        model.onGround = 0.0F;
        model.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);
        model.bipedRightArm.render(0.0625F);
        model.renderSleeve(0.0625F);
    }

    private ModelBase pickModel(Entity entity) {
        if(!(entity instanceof AbstractClientPlayer)) return vanillaModel;
        Boolean slim = ModernSkins.slimFor(ModernSkins.keyFor((AbstractClientPlayer) entity));
        if(slim == null) return vanillaModel;
        return slim.booleanValue() ? slimModel : classicModel;
    }
}
