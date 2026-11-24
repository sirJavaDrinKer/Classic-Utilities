package classic_utilities.forge.entity;

import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;

public class EntityFreecam extends EntityLiving {
	public EntityFreecam(World world) {
		super(world);
		this.noClip = true;
		this.yOffset = 1.62F;
	}

	@Override
	public float getEyeHeight() {
		return 0.0F;
	}
}
