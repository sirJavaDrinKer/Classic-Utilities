package classic_utilities.forge.feature;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

public class NoFogController {
    private static final int FOG_COORDINATE_SOURCE = 0x8450;
    private static final int FOG_COORDINATE = 0x8451;
    private static final int FRAGMENT_DEPTH = 0x8452; // the GL default

    private final Minecraft client;

    private boolean supportChecked;
    private boolean supported;
    private boolean applied;

    public NoFogController(Minecraft client) {
        this.client = client;
    }

    public boolean isSupported() {
        if(!supportChecked) {
            supportChecked = true;
            supported = GLContext.getCapabilities().OpenGL14;
        }
        return supported;
    }

    public void apply(boolean wanted) {
        boolean active = wanted && client.theWorld != null && !cameraInFluid();

        if(!active && !applied) return;
        if(!isSupported()) return;

        GL11.glFogi(FOG_COORDINATE_SOURCE, active ? FOG_COORDINATE : FRAGMENT_DEPTH);
        applied = active;
    }

    private boolean cameraInFluid() {
        if(client.renderViewEntity == null) return false;
        return client.renderViewEntity.isInsideOfMaterial(Material.water)
                || client.renderViewEntity.isInsideOfMaterial(Material.lava);
    }
}
