package classic_utilities.forge.feature;

import classic_utilities.math.ZoomMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.input.Mouse;

public class ZoomController {
    private final Minecraft client;

    private boolean overridden;
    private float savedFov;
    private float savedSensitivity;
    private int lastHotbarSlot;

    public ZoomController(Minecraft client) {
        this.client = client;
    }

    public void apply(float factor) {
        GameSettings gs = client.gameSettings;

        if(!overridden) {
            savedFov = gs.fovSetting;
            savedSensitivity = gs.mouseSensitivity;
            overridden = true;
        }

        gs.fovSetting = ZoomMath.fovSetting(savedFov, factor);
        gs.mouseSensitivity = ZoomMath.mouseSensitivity(savedSensitivity, factor);
    }

    public void restore() {
        if(!overridden) return;
        overridden = false;

        GameSettings gs = client.gameSettings;
        gs.fovSetting = savedFov;
        gs.mouseSensitivity = savedSensitivity;
    }

    public int consumeScroll(boolean zoomActive) {
        int wheel = Mouse.getDWheel();

        EntityClientPlayerMP player = client.thePlayer;
        if(player == null) return 0;

        if(zoomActive && wheel != 0 && client.currentScreen == null) {
            player.inventory.currentItem = lastHotbarSlot;

            int notches = Math.max(1, Math.abs(wheel) / 120);
            return wheel > 0 ? notches : -notches;
        }

        lastHotbarSlot = player.inventory.currentItem;
        return 0;
    }
}
