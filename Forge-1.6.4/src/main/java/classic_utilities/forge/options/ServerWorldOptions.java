package classic_utilities.forge.options;

import classic_utilities.network.OptionsProtocol;
import classic_utilities.options.WorldOptions;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServerWorldOptions {
    public static final ServerWorldOptions INSTANCE = new ServerWorldOptions();

    private static final Logger LOG = Logger.getLogger("ForgeModLoader");
    private static final String SAVE_NAME = "cu_options";

    private volatile WorldOptions options = WorldOptions.DEFAULTS;
    private File saveFile;

    private ServerWorldOptions() {
    }

    public WorldOptions get() {
        return options;
    }

    public synchronized void load(MinecraftServer server) {
        options = WorldOptions.DEFAULTS;
        saveFile = null;

        WorldServer overworld = server.worldServerForDimension(0);
        if(overworld != null) {
            saveFile = overworld.getSaveHandler().getMapFileFromName(SAVE_NAME);
        }

        if(saveFile != null && saveFile.isFile()) {
            Properties props = new Properties();
            FileInputStream in = null;
            try {
                in = new FileInputStream(saveFile);
                props.load(in);

                WorldOptions loaded = WorldOptions.DEFAULTS;
                for(int id = 0; id < WorldOptions.COUNT; id++) {
                    String value = props.getProperty(WorldOptions.key(id));
                    if(value != null) loaded = loaded.with(id, Boolean.parseBoolean(value.trim()));
                }
                options = loaded;
            } catch(IOException e) {
                LOG.log(Level.WARNING, "[CUtil] could not read world options, using defaults", e);
            } finally {
                close(in);
            }
        }

        EndermanGriefing.set(options.allowEndermanGriefing);
        AnvilLimit.set(options.removeAnvilLimit);
    }

    private void save() {
        if(saveFile == null) return;

        Properties props = new Properties();
        for(int id = 0; id < WorldOptions.COUNT; id++) {
            props.setProperty(WorldOptions.key(id), Boolean.toString(options.get(id)));
        }

        FileOutputStream out = null;
        try {
            File parent = saveFile.getParentFile();
            if(parent != null && !parent.exists()) parent.mkdirs();
            out = new FileOutputStream(saveFile);
            props.store(out, "Classic Utilities world options");
        } catch(IOException e) {
            LOG.log(Level.WARNING, "[CUtil] could not save world options", e);
        } finally {
            close(out);
        }
    }

    private static void close(java.io.Closeable c) {
        if(c == null) return;
        try {
            c.close();
        } catch(IOException ignored) {
        }
    }

    public static boolean canEdit(EntityPlayerMP player) {
        MinecraftServer server = MinecraftServer.getServer();
        if(server == null) return false;

        String owner = server.getServerOwner();
        if(owner != null && owner.equalsIgnoreCase(player.username)) {
            return true;
        }

        return server.getConfigurationManager().isPlayerOpped(player.username);
    }

    public synchronized void handleSet(EntityPlayerMP player, int id, boolean value) {
        if(!canEdit(player)) {
            sendState(player);
            return;
        }

        options = options.with(id, value);
        if(id == WorldOptions.ENDERMAN_GRIEFING) EndermanGriefing.set(value);
        if(id == WorldOptions.REMOVE_ANVIL_LIMIT) AnvilLimit.set(value);
        save();
        broadcast();
    }

    public void sendState(EntityPlayerMP player) {
        try {
            byte[] payload = OptionsProtocol.encodeState(canEdit(player), options);

            Packet250CustomPayload packet = new Packet250CustomPayload();
            packet.channel = OptionsProtocol.CHANNEL;
            packet.data = payload;
            packet.length = payload.length;

            PacketDispatcher.sendPacketToPlayer(packet, (Player) player);
        } catch(IOException e) {
            LOG.log(Level.WARNING, "[CUtil] could not encode world options", e);
        }
    }

    private void broadcast() {
        MinecraftServer server = MinecraftServer.getServer();
        if(server == null) return;

        List players = server.getConfigurationManager().playerEntityList;
        for(Object o : players) {
            if(o instanceof EntityPlayerMP) sendState((EntityPlayerMP) o);
        }
    }
}
