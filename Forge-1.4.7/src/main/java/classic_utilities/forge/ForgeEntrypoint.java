package classic_utilities.forge;

import cpw.mods.fml.common.Mod;
import classic_utilities.options.ClientOptions;
import classic_utilities.forge.options.OptionsPacketHandler;
import classic_utilities.forge.options.OptionsPlayerTracker;
import classic_utilities.forge.options.OptionsTicker;
import classic_utilities.forge.options.ServerWorldOptions;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import classic_utilities.forge.enchant.EnchantPacketHandler;
import classic_utilities.forge.enchant.EnchantSync;
import classic_utilities.network.EnchantProtocol;
import classic_utilities.network.OptionsProtocol;
import classic_utilities.update.UpdateChecker;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

@Mod(
        modid = "classic_utilities",
        useMetadata = true
)
public class ForgeEntrypoint {
    @Mod.PreInit
    public void preinit(FMLPreInitializationEvent e) {
        NetworkRegistry.instance().registerChannel(new EnchantPacketHandler(), EnchantProtocol.CHANNEL);
        TickRegistry.registerTickHandler(EnchantSync.INSTANCE, Side.SERVER);

        NetworkRegistry.instance().registerChannel(new OptionsPacketHandler(), OptionsProtocol.CHANNEL);
        TickRegistry.registerTickHandler(new OptionsTicker(), Side.SERVER);
        GameRegistry.registerPlayerTracker(new OptionsPlayerTracker());

        if(e.getSide() == Side.CLIENT) {
            ClientOptions.load(new File(e.getModConfigurationDirectory(), "classic_utilities_client.properties"));
            UpdateChecker.start(e.getModMetadata().version, "1.4.7");
            ClientOnly.start();
        }
    }

    @Mod.ServerStarting
    public void serverStarting(FMLServerStartingEvent e) {
        ServerWorldOptions.INSTANCE.load(e.getServer());
    }

    // I just ate some goated chipotle
    private static class ClientOnly {
        public static void start() {
            new ForgeImpl().init();
        }
    }
}
