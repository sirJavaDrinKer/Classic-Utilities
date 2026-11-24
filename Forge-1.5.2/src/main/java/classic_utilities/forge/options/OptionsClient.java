package classic_utilities.forge.options;

import classic_utilities.network.OptionsProtocol;
import cpw.mods.fml.common.network.PacketDispatcher;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.network.packet.Packet250CustomPayload;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class OptionsClient {
    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    private OptionsClient() {
    }

    public static void sendQuery() {
        send(OptionsProtocol.encodeQuery());
    }

    public static void sendSet(int id, boolean value) {
        try {
            send(OptionsProtocol.encodeSet(id, value));
        } catch(IOException e) {
            LOG.log(Level.WARNING, "[CUtil] could not encode options change", e);
        }
    }

    private static void send(byte[] payload) {
        Packet250CustomPayload packet = new Packet250CustomPayload();
        packet.channel = OptionsProtocol.CHANNEL;
        packet.data = payload;
        packet.length = payload.length;
        PacketDispatcher.sendPacketToServer(packet);
    }
}