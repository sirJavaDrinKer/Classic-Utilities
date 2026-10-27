package classic_utilities.forge;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;

@Mod(
        modid = "classic_utilities",
        useMetadata = true
)
public class ForgeEntrypoint {
    @Mod.PreInit
    public void preinit(FMLPreInitializationEvent e) {
        if(e.getSide() == Side.CLIENT) {
            ClientOnly.start();
        }
    }

    // I just ate some goated chipotle x2
    private static class ClientOnly {
        public static void start() {
            new ForgeImpl().init();
        }
    }
}
