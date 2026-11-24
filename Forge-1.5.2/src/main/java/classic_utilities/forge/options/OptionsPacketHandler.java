package classic_utilities.forge.options;

import classic_utilities.network.OptionsProtocol;
import classic_utilities.options.ClientWorldOptions;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OptionsPacketHandler implements IPacketHandler {
    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    @Override
    public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player) {
        if(!OptionsProtocol.CHANNEL.equals(packet.channel)) return;

        try {
            if(player instanceof EntityPlayerMP) {
                handleOnServer((EntityPlayerMP) player, packet.data);
            } else {
                handleOnClient(packet.data);
            }
        } catch(IOException e) {
            LOG.log(Level.WARNING, "[CUtil] ignoring malformed options packet", e);
        }
    }

    private void handleOnServer(EntityPlayerMP player, byte[] data) throws IOException {
        int type = OptionsProtocol.type(data);
        if(type == OptionsProtocol.TYPE_QUERY) {
            ServerWorldOptions.INSTANCE.sendState(player);
        } else if(type == OptionsProtocol.TYPE_SET) {
            OptionsProtocol.SetRequest request = OptionsProtocol.decodeSet(data);
            if(request != null) ServerWorldOptions.INSTANCE.handleSet(player, request.id, request.value);
        }
    }

    private void handleOnClient(byte[] data) throws IOException {
        OptionsProtocol.ServerState state = OptionsProtocol.decodeState(data);
        if(state != null) ClientWorldOptions.update(state.canEdit, state.options);
    }
}
