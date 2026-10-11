package classic_utilities.forge;

import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.EnumSet;

public class ForgeImpl extends ClassicUtilities147 {
	private final KeyBinding FREECAM_KEY = new KeyBinding("classic_utilities.freecam", Keyboard.KEY_F6);
	private final KeyBinding FULLBRIGHT_KEY = new KeyBinding("classic_utilities.fullbright", Keyboard.KEY_F7);
	
	private boolean freecamKeyWasDown;
	private boolean fullbrightKeyWasDown;
	
	public void init() {
		LanguageRegistry.instance().loadLocalization("/assets/classic_utilities/lang/en_US.lang", "en_US", false);

		TickRegistry.registerTickHandler(new Ticker(), Side.CLIENT);

		KeyBindingRegistry.registerKeyBinding(new KeyBindingRegistry.KeyHandler(new KeyBinding[] { FREECAM_KEY, FULLBRIGHT_KEY }) {
			@Override
			public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat) {
				//Dont care
			}
			
			@Override
			public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) {
				//Dont care
			}
			
			@Override
			public EnumSet<TickType> ticks() {
				return EnumSet.noneOf(TickType.class);
			}
			
			@Override
			public String getLabel() {
				return "Classic Utility key handler thing";
			}
		});
	}

	private void pollKeys() {
		boolean freecamDown = FREECAM_KEY.pressed;
		boolean fullbrightDown = FULLBRIGHT_KEY.pressed;
		
		if(client.currentScreen == null && client.thePlayer != null && client.theWorld != null) {
			if(freecamDown && !freecamKeyWasDown) toggleFreecam();
			if(fullbrightDown && !fullbrightKeyWasDown) toggleFullbright();
		}
		
		freecamKeyWasDown = freecamDown;
		fullbrightKeyWasDown = fullbrightDown;
	}
	
	private class Ticker implements ITickHandler {
		private final EnumSet<TickType> ticks = EnumSet.of(TickType.CLIENT, TickType.RENDER);
		
		@Override
		public void tickStart(EnumSet<TickType> types, Object... args) {
			if(types.contains(TickType.RENDER)) {
				pollKeys();
				renderTick();
			}
			if(types.contains(TickType.CLIENT)) clientTickStart();
		}
		
		@Override
		public void tickEnd(EnumSet<TickType> types, Object... args) {
			if(types.contains(TickType.RENDER)) renderTick();
			if(types.contains(TickType.CLIENT)) clientTickEnd();
		}
		
		@Override
		public EnumSet<TickType> ticks() {
			return ticks;
		}
		
		@Override
		public String getLabel() {
			return "Classic Utilities client ticker";
		}
	}
}
