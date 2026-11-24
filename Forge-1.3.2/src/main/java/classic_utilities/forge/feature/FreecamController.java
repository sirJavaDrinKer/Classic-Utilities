package classic_utilities.forge.feature;

import classic_utilities.forge.entity.EntityFreecam;
import classic_utilities.math.FreecamMath;
import classic_utilities.options.ClientWorldOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.EntityClientPlayerMP;
import net.minecraft.src.GameSettings;
import net.minecraft.src.MovementInput;
import org.lwjgl.input.Keyboard;

import java.util.List;

import static classic_utilities.ClassicUtilities.FREECAM_BOOST;
import static classic_utilities.ClassicUtilities.FREECAM_COLLISION_RADIUS;
import static classic_utilities.ClassicUtilities.FREECAM_SPEED;

public class FreecamController {
    private final Minecraft client;

    private EntityFreecam camera;
    private EntityClientPlayerMP trackedPlayer;
    private MovementInput savedInput;
    private float frozenYaw, frozenPitch;

    public FreecamController(Minecraft client) {
        this.client = client;
    }

    public boolean enable() {
        EntityClientPlayerMP player = client.thePlayer;
        if(player == null || client.theWorld == null) return false;

        EntityFreecam cam = new EntityFreecam(client.theWorld);
        cam.setPosition(player.posX, player.posY, player.posZ);
        cam.prevPosX = cam.lastTickPosX = player.posX;
        cam.prevPosY = cam.lastTickPosY = player.posY;
        cam.prevPosZ = cam.lastTickPosZ = player.posZ;
        cam.rotationYaw = cam.prevRotationYaw = player.rotationYaw;
        cam.rotationPitch = cam.prevRotationPitch = player.rotationPitch;

        camera = cam;
        trackedPlayer = player;
        frozenYaw = player.rotationYaw;
        frozenPitch = player.rotationPitch;

        savedInput = player.movementInput;
        player.movementInput = new MovementInput();

        client.renderViewEntity = cam;
        return true;
    }

    public void disable() {
        if(trackedPlayer != null && savedInput != null) {
            trackedPlayer.movementInput = savedInput;
        }
        if(camera != null && client.renderViewEntity == camera && client.thePlayer != null) {
            client.renderViewEntity = client.thePlayer;
        }
        camera = null;
        trackedPlayer = null;
        savedInput = null;
    }

    public boolean isValid() {
        return camera != null
                && client.thePlayer != null
                && client.thePlayer == trackedPlayer
                && trackedPlayer.isEntityAlive()
                && client.theWorld != null
                && client.theWorld == camera.worldObj
                && client.renderViewEntity == camera;
    }

    public void tickStart() {
        client.objectMouseOver = null;
    }

    public void tickEnd() {
        if(camera == null) return;

        camera.prevPosX = camera.lastTickPosX = camera.posX;
        camera.prevPosY = camera.lastTickPosY = camera.posY;
        camera.prevPosZ = camera.lastTickPosZ = camera.posZ;

        if(client.currentScreen != null) return;

        GameSettings gs = client.gameSettings;
        double forward = 0, strafe = 0, vertical = 0;
        if(gs.keyBindForward.pressed) forward += 1;
        if(gs.keyBindBack.pressed) forward -= 1;
        if(gs.keyBindLeft.pressed) strafe += 1;
        if(gs.keyBindRight.pressed) strafe -= 1;
        if(gs.keyBindJump.pressed) vertical += 1;
        if(gs.keyBindSneak.pressed) vertical -= 1;

        double speed = FREECAM_SPEED * (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ? FREECAM_BOOST : 1.0D);
        double[] v = FreecamMath.velocity(camera.rotationYaw, camera.rotationPitch, forward, strafe, vertical, speed);

        if(ClientWorldOptions.isFreecamCollisionEnabled()) {
            v = collide(v);
        }

        camera.setPosition(camera.posX + v[0], camera.posY + v[1], camera.posZ + v[2]);
    }

    private double[] collide(double[] v) {
        double r = FREECAM_COLLISION_RADIUS;
        double dx = v[0], dy = v[1], dz = v[2];

        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                camera.posX - r, camera.posY - r, camera.posZ - r,
                camera.posX + r, camera.posY + r, camera.posZ + r);
        List hits = client.theWorld.getCollidingBoundingBoxes(camera, box.addCoord(dx, dy, dz));

        for(Object o : hits) dy = ((AxisAlignedBB) o).calculateYOffset(box, dy);
        box.offset(0.0D, dy, 0.0D);

        for(Object o : hits) dx = ((AxisAlignedBB) o).calculateXOffset(box, dx);
        box.offset(dx, 0.0D, 0.0D);

        for(Object o : hits) dz = ((AxisAlignedBB) o).calculateZOffset(box, dz);

        return new double[] { dx, dy, dz };
    }

    public void renderTick() {
        if(camera == null || trackedPlayer == null) return;

        float dYaw = trackedPlayer.rotationYaw - frozenYaw;
        float dPitch = trackedPlayer.rotationPitch - frozenPitch;

        if(dYaw != 0.0F || dPitch != 0.0F) {
            camera.rotationYaw += dYaw;
            float p = camera.rotationPitch + dPitch;
            if(p > 90.0F) p = 90.0F;
            if(p < -90.0F) p = -90.0F;
            camera.rotationPitch = p;

            trackedPlayer.rotationYaw = trackedPlayer.prevRotationYaw = frozenYaw;
            trackedPlayer.rotationPitch = trackedPlayer.prevRotationPitch = frozenPitch;
        }

        camera.prevRotationYaw = camera.rotationYaw;
        camera.prevRotationPitch = camera.rotationPitch;
    }
}
