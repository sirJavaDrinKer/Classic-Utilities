package classic_utilities.forge.options;

import cpw.mods.fml.common.IPlayerTracker;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;

public class OptionsPlayerTracker implements IPlayerTracker {
    @Override
    public void onPlayerLogin(EntityPlayer player) {
        send(player);
    }

    @Override
    public void onPlayerLogout(EntityPlayer player) {
    }

    @Override
    public void onPlayerChangedDimension(EntityPlayer player) {
        send(player);
    }

    @Override
    public void onPlayerRespawn(EntityPlayer player) {
        send(player);
    }

    private void send(EntityPlayer player) {
        if(player instanceof EntityPlayerMP) ServerWorldOptions.INSTANCE.sendState((EntityPlayerMP) player);
    }
}
