package classic_utilities.forge;

import classic_utilities.EnchantProtocol;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.NetworkManager;
import net.minecraft.src.Packet250CustomPayload;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EnchantPacketHandler implements IPacketHandler {
    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    @Override
    public void onPacketData(NetworkManager manager, Packet250CustomPayload packet, Player player) {
        if(!EnchantProtocol.CHANNEL.equals(packet.channel)) return;
        if(player instanceof EntityPlayerMP) return;

        try {
            ClientEnchantState.set(EnchantProtocol.decode(packet.data));
        } catch(IOException e) {
            LOG.log(Level.WARNING, "Classic Utilities: ignoring malformed enchant packet", e);
        }
    }
}
