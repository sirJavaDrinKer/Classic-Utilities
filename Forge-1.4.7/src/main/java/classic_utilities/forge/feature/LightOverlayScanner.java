package classic_utilities.forge.feature;

import classic_utilities.world.SpawnScan;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.World;

import static classic_utilities.ClassicUtilities.OVERLAY_IGNORE_SKYLIGHT;
import static classic_utilities.ClassicUtilities.OVERLAY_RADIUS;
import static classic_utilities.ClassicUtilities.OVERLAY_VERTICAL_RADIUS;
import static classic_utilities.ClassicUtilities.SPAWN_MAX_LIGHT;

public class LightOverlayScanner {
    private final Minecraft client;
    private final SpawnScan spawnScan;

    public LightOverlayScanner(Minecraft client, SpawnScan spawnScan) {
        this.client = client;
        this.spawnScan = spawnScan;
    }

    public void scan() {
        EntityLiving view = client.renderViewEntity;
        World world = client.theWorld;
        if(view == null || world == null) {
            spawnScan.clear();
            return;
        }

        int cx = MathHelper.floor_double(view.posX);
        int cy = MathHelper.floor_double(view.posY - view.yOffset); //feet
        int cz = MathHelper.floor_double(view.posZ);
        spawnScan.scan(new MobSpawnQuery(world), cx, cy, cz, OVERLAY_RADIUS, OVERLAY_VERTICAL_RADIUS);
    }

    private static class MobSpawnQuery implements SpawnScan.Query {
        private final World world;

        MobSpawnQuery(World world) {
            this.world = world;
        }

        @Override
        public boolean canSpawn(int x, int y, int z) {
            if(world.getBlockId(x, y - 1, z) == 0) return false;

            int light = OVERLAY_IGNORE_SKYLIGHT
                    ? world.getSavedLightValue(EnumSkyBlock.Block, x, y, z)
                    : world.getBlockLightValue(x, y, z);
            if(light > SPAWN_MAX_LIGHT) return false;


            return SpawnerAnimals.canCreatureTypeSpawnAtLocation(EnumCreatureType.monster, world, x, y, z);
        }
    }
}
