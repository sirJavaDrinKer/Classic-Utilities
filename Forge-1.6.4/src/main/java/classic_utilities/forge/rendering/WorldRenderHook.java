package classic_utilities.forge.rendering;

import classic_utilities.ClassicUtilities;
import classic_utilities.options.ClientOptions;
import classic_utilities.rendering.ChunkBorderRenderer;
import classic_utilities.rendering.HitboxRenderer;
import classic_utilities.rendering.LightOverlayRenderer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;

public class WorldRenderHook {
    private final ClassicUtilities mod;
    private final LightOverlayRenderer lightOverlayRenderer;
    private final ChunkBorderRenderer chunkBorderRenderer;
    private final HitboxRenderer hitboxRenderer;

    public WorldRenderHook(ClassicUtilities mod, LightOverlayRenderer lightOverlayRenderer, ChunkBorderRenderer chunkBorderRenderer, HitboxRenderer hitboxRenderer) {
        this.mod = mod;
        this.lightOverlayRenderer = lightOverlayRenderer;
        this.chunkBorderRenderer = chunkBorderRenderer;
        this.hitboxRenderer = hitboxRenderer;
    }

    @ForgeSubscribe
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if(mod.isLightOverlay()) lightOverlayRenderer.render(event.partialTicks);
        if(mod.isChunkBorders()) chunkBorderRenderer.render(event.partialTicks);
        if(mod.isHitboxes() && ClientOptions.isBetterHitboxes()) hitboxRenderer.render(event.partialTicks);
    }
}
