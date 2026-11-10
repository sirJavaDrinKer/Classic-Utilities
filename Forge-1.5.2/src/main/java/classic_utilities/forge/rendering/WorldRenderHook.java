package classic_utilities.forge.rendering;

import classic_utilities.ClassicUtilities;
import classic_utilities.rendering.ChunkBorderRenderer;
import classic_utilities.rendering.LightOverlayRenderer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;

public class WorldRenderHook {
    private final ClassicUtilities mod;
    private final LightOverlayRenderer lightOverlayRenderer;
    private final ChunkBorderRenderer chunkBorderRenderer;

    public WorldRenderHook(ClassicUtilities mod, LightOverlayRenderer lightOverlayRenderer, ChunkBorderRenderer chunkBorderRenderer) {
        this.mod = mod;
        this.lightOverlayRenderer = lightOverlayRenderer;
        this.chunkBorderRenderer = chunkBorderRenderer;
    }

    @ForgeSubscribe
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if(mod.isLightOverlay()) lightOverlayRenderer.render(event.partialTicks);
        if(mod.isChunkBorders()) chunkBorderRenderer.render(event.partialTicks);
    }
}
