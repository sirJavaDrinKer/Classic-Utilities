package classic_utilities.forge;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;


public class LightOverlayHook {
    private final ClassicUtilities147 mod;

    public LightOverlayHook(ClassicUtilities147 mod) {
        this.mod = mod;
    }

    @ForgeSubscribe
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        mod.renderLightOverlay(event.partialTicks);
    }
}