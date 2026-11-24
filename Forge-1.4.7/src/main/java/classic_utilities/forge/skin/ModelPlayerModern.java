package classic_utilities.forge.skin;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

public class ModelPlayerModern extends ModelBiped {
    private static final float SCALE_LAYER = 0.25F;

    public final ModelRenderer bipedJacket;
    public final ModelRenderer bipedLeftSleeve;
    public final ModelRenderer bipedRightSleeve;
    public final ModelRenderer bipedLeftPants;
    public final ModelRenderer bipedRightPants;

    private final ModelBiped mirror;
    private final float armY;

    public ModelPlayerModern(ModelBiped mirror, boolean slim) {
        super(0.0F, 0.0F, 64, 64);
        this.mirror = mirror;

        int armWidth = slim ? 3 : 4;
        this.armY = slim ? 2.5F : 2.0F;
        float rightArmX = slim ? -2.0F : -3.0F;

        // Base layer: right arm / leg keep vanilla's texture spots, the left ones get their own.
        bipedRightArm = new ModelRenderer(this, 40, 16);
        bipedRightArm.addBox(rightArmX, -2.0F, -2.0F, armWidth, 12, 4, 0.0F);
        bipedRightArm.setRotationPoint(-5.0F, armY, 0.0F);

        bipedLeftArm = new ModelRenderer(this, 32, 48);
        bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, armWidth, 12, 4, 0.0F);
        bipedLeftArm.setRotationPoint(5.0F, armY, 0.0F);

        float legX = -bipedRightLeg.rotationPointX;
        bipedLeftLeg = new ModelRenderer(this, 16, 48);
        bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
        bipedLeftLeg.setRotationPoint(legX, bipedRightLeg.rotationPointY, 0.0F);

        // Second layer.
        bipedJacket = new ModelRenderer(this, 16, 32);
        bipedJacket.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, SCALE_LAYER);

        bipedRightSleeve = new ModelRenderer(this, 40, 32);
        bipedRightSleeve.addBox(rightArmX, -2.0F, -2.0F, armWidth, 12, 4, SCALE_LAYER);

        bipedLeftSleeve = new ModelRenderer(this, 48, 48);
        bipedLeftSleeve.addBox(-1.0F, -2.0F, -2.0F, armWidth, 12, 4, SCALE_LAYER);

        bipedRightPants = new ModelRenderer(this, 0, 32);
        bipedRightPants.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, SCALE_LAYER);

        bipedLeftPants = new ModelRenderer(this, 0, 48);
        bipedLeftPants.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, SCALE_LAYER);
    }

    @Override
    public void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float scale) {
        setRotationAngles(f1, f2, f3, f4, f5, scale, entity);

        bipedHead.render(scale);
        bipedBody.render(scale);
        bipedRightArm.render(scale);
        bipedLeftArm.render(scale);
        bipedRightLeg.render(scale);
        bipedLeftLeg.render(scale);
        bipedHeadwear.render(scale);

        renderSecondLayer(scale);
    }

    /** Draws jacket, sleeves and pants. */
    public void renderSecondLayer(float scale) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        bipedJacket.render(scale);
        bipedRightSleeve.render(scale);
        bipedLeftSleeve.render(scale);
        bipedRightPants.render(scale);
        bipedLeftPants.render(scale);
        GL11.glDisable(GL11.GL_BLEND);
    }

    /** Just the right sleeve, for the first person arm. */
    public void renderSleeve(float scale) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        bipedRightSleeve.render(scale);
        GL11.glDisable(GL11.GL_BLEND);
    }

    @Override
    public void setRotationAngles(float f1, float f2, float f3, float f4, float f5, float f6, Entity entity) {
        // Item and sneak state is set by vanilla on its own model, take it from there.
        heldItemLeft = mirror.heldItemLeft;
        heldItemRight = mirror.heldItemRight;
        isSneak = mirror.isSneak;
        aimedBow = mirror.aimedBow;

        super.setRotationAngles(f1, f2, f3, f4, f5, f6, entity);

        // The vanilla pose code only places the arms for the classic 2.0 shoulder height.
        bipedRightArm.rotationPointY = armY;
        bipedLeftArm.rotationPointY = armY;

        copyPose(bipedBody, bipedJacket);
        copyPose(bipedRightArm, bipedRightSleeve);
        copyPose(bipedLeftArm, bipedLeftSleeve);
        copyPose(bipedRightLeg, bipedRightPants);
        copyPose(bipedLeftLeg, bipedLeftPants);

        // Keep vanilla's model posed identically, it is used for held items, capes and the like.
        mirror.onGround = onGround;
        mirror.isRiding = isRiding;
        mirror.isChild = isChild;
        mirror.setRotationAngles(f1, f2, f3, f4, f5, f6, entity);
    }

    private static void copyPose(ModelRenderer from, ModelRenderer to) {
        to.rotationPointX = from.rotationPointX;
        to.rotationPointY = from.rotationPointY;
        to.rotationPointZ = from.rotationPointZ;
        to.rotateAngleX = from.rotateAngleX;
        to.rotateAngleY = from.rotateAngleY;
        to.rotateAngleZ = from.rotateAngleZ;
    }
}