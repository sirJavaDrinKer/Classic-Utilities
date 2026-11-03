package classic_utilities.forge;

import cpw.mods.fml.common.Mod;
import classic_utilities.EnchantProtocol;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.common.Side;

@Mod(
        modid = "classic_utilities",
        useMetadata = true
)
public class ForgeEntrypoint {
    @Mod.PreInit
    public void preinit(FMLPreInitializationEvent e) {
        NetworkRegistry.instance().registerChannel(new EnchantPacketHandler(), EnchantProtocol.CHANNEL);
        TickRegistry.registerTickHandler(EnchantSync.INSTANCE, Side.SERVER);

        if(e.getSide() == Side.CLIENT) {
            ClientOnly.start();
        }
    }

    // I just ate some goated chipotle
    private static class ClientOnly {
        public static void start() {
            new ForgeImpl().init();
        }
    }
}
