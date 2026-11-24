package classic_utilities.forge.skin;

import net.minecraft.src.ModelBiped;
import net.minecraft.src.ModelRenderer;
import net.minecraft.src.Entity;
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
        super(0.0F, 0.0F);
        this.mirror = mirror;

        int armWidth = slim ? 3 : 4;
        this.armY = slim ? 2.5F : 2.0F;
        float rightArmX = slim ? -2.0F : -3.0F;

        bipedHead = part(0, 0);
        bipedHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, 0.0F);
        bipedHead.setRotationPoint(0.0F, 0.0F, 0.0F);

        bipedHeadwear = part(32, 0);
        bipedHeadwear.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, 0.5F);
        bipedHeadwear.setRotationPoint(0.0F, 0.0F, 0.0F);

        bipedBody = part(16, 16);
        bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, 0.0F);
        bipedBody.setRotationPoint(0.0F, 0.0F, 0.0F);

        bipedRightLeg = part(0, 16);
        bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
        bipedRightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);

        bipedRightArm = part(40, 16);
        bipedRightArm.addBox(rightArmX, -2.0F, -2.0F, armWidth, 12, 4, 0.0F);
        bipedRightArm.setRotationPoint(-5.0F, armY, 0.0F);

        bipedLeftArm = part(32, 48);
        bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, armWidth, 12, 4, 0.0F);
        bipedLeftArm.setRotationPoint(5.0F, armY, 0.0F);

        float legX = -bipedRightLeg.rotationPointX;
        bipedLeftLeg = part(16, 48);
        bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
        bipedLeftLeg.setRotationPoint(legX, bipedRightLeg.rotationPointY, 0.0F);

        bipedJacket = part(16, 32);
        bipedJacket.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, SCALE_LAYER);

        bipedRightSleeve = part(40, 32);
        bipedRightSleeve.addBox(rightArmX, -2.0F, -2.0F, armWidth, 12, 4, SCALE_LAYER);

        bipedLeftSleeve = part(48, 48);
        bipedLeftSleeve.addBox(-1.0F, -2.0F, -2.0F, armWidth, 12, 4, SCALE_LAYER);

        bipedRightPants = part(0, 32);
        bipedRightPants.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, SCALE_LAYER);

        bipedLeftPants = part(0, 48);
        bipedLeftPants.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, SCALE_LAYER);
    }

    private ModelRenderer part(int u, int v) {
        ModelRenderer renderer = new ModelRenderer(this, u, v);
        renderer.setTextureSize(64, 64); // must happen before addBox, which bakes the UVs
        return renderer;
    }

    @Override
    public void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float scale) {
        setRotationAngles(f1, f2, f3, f4, f5, scale);

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
    public void setRotationAngles(float f1, float f2, float f3, float f4, float f5, float f6) {
        heldItemLeft = mirror.heldItemLeft;
        heldItemRight = mirror.heldItemRight;
        isSneak = mirror.isSneak;
        aimedBow = mirror.aimedBow;

        super.setRotationAngles(f1, f2, f3, f4, f5, f6);

        bipedRightArm.rotationPointY = armY;
        bipedLeftArm.rotationPointY = armY;

        copyPose(bipedBody, bipedJacket);
        copyPose(bipedRightArm, bipedRightSleeve);
        copyPose(bipedLeftArm, bipedLeftSleeve);
        copyPose(bipedRightLeg, bipedRightPants);
        copyPose(bipedLeftLeg, bipedLeftPants);

        mirror.onGround = onGround;
        mirror.isRiding = isRiding;
        mirror.isChild = isChild;
        mirror.setRotationAngles(f1, f2, f3, f4, f5, f6);
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