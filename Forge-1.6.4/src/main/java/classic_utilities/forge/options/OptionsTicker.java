package classic_utilities.forge.options;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class OptionsTicker implements ITickHandler {
    private static final int CHECK_INTERVAL = 20;

    private final EnumSet<TickType> ticks = EnumSet.of(TickType.WORLD);
    private int counter;

    @Override
    public void tickStart(EnumSet<TickType> types, Object... tickData) {
    }

    @Override
    public void tickEnd(EnumSet<TickType> types, Object... tickData) {
        if(tickData.length == 0 || !(tickData[0] instanceof World)) return;

        World world = (World) tickData[0];
        if(world.isRemote) return;
        if(ServerWorldOptions.INSTANCE.get().allowEndermanGriefing) return;
        if(++counter < CHECK_INTERVAL) return;
        counter = 0;

        List entities = new ArrayList(world.loadedEntityList);
        for(Object o : entities) {
            if(o instanceof EntityEnderman) {
                EntityEnderman enderman = (EntityEnderman) o;
                if(enderman.getCarried() > 0) enderman.setCarried(0);
            }
        }
    }

    @Override
    public EnumSet<TickType> ticks() {
        return ticks;
    }

    @Override
    public String getLabel() {
        return "Classic Utilities world options";
    }
}
