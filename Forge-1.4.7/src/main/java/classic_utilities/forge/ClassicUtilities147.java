package classic_utilities.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MovementInput;
import org.lwjgl.input.Keyboard;
import classic_utilities.FreecamMath;
import classic_utilities.ClassicUtilities;

// holy balls batman
public abstract class ClassicUtilities147 extends ClassicUtilities {
	protected final Minecraft client = Minecraft.getMinecraft();
	
	private EntityFreecam camera;
	private EntityClientPlayerMP trackedPlayer;
	private MovementInput savedInput;
	private float frozenYaw, frozenPitch;
	
	@Override
	protected float getGamma() {
		return client.gameSettings.gammaSetting;
	}
	
	@Override
	protected void setGamma(float gamma) {
		client.gameSettings.gammaSetting = gamma;
	}
	
	@Override
	protected void say(String message) {
		if(client.ingameGUI != null) {
			client.ingameGUI.getChatGUI().printChatMessage("\u00a7e[CUtil] \u00a7f" + message);
		}
	}
	
	@Override
	protected boolean freecamEnable() {
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
	
	@Override
	protected void freecamDisable() {
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

	@Override
	protected boolean freecamValid() {
		return camera != null
			&& client.thePlayer != null
			&& client.thePlayer == trackedPlayer
			&& trackedPlayer.isEntityAlive()
			&& client.theWorld != null
			&& client.theWorld == camera.worldObj
			&& client.renderViewEntity == camera;
	}
	
	@Override
	protected void freecamTickStart() {
		client.objectMouseOver = null;
	}
	
	@Override
	protected void freecamTickEnd() {
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
		
		camera.setPosition(camera.posX + v[0], camera.posY + v[1], camera.posZ + v[2]);
	}

	@Override
	protected void freecamRenderTick() {
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
