package classic_utilities;

/**
 * This class must remain compatible with Java 6.
 */
public abstract class ClassicUtilities {
	public static final String MODID = "classic_utilities";
	public static final String NAME = "Classic Utilities";
	
	public static ClassicUtilities instance;
	
	//Tunables
	public static final double FREECAM_SPEED = 0.6D;
	public static final double FREECAM_BOOST = 3.0D;
	public static final float FULLBRIGHT_GAMMA = 16.0F;
	
	private boolean freecam;
	private boolean fullbright;
	private float savedGamma = 1.0F;
	private boolean gammaChecked;
	
	public ClassicUtilities() {
		if(instance == null) {
			instance = this;
		} else {
			throw new IllegalStateException(NAME + " instantiated twice!");
		}
	}
	
	protected abstract float getGamma();
	protected abstract void setGamma(float gamma);
	protected abstract void say(String message);

	protected abstract boolean freecamEnable();
	protected abstract void freecamDisable();
	protected abstract boolean freecamValid();
	protected abstract void freecamTickStart();
	protected abstract void freecamTickEnd();
	protected abstract void freecamRenderTick();
	
	public boolean isFreecam() {
		return freecam;
	}
	
	public boolean isFullbright() {
		return fullbright;
	}
	
	public void toggleFreecam() {
		if(freecam) {
			freecam = false;
			freecamDisable();
			say("Freecam: OFF");
		} else if(freecamEnable()) {
			freecam = true;
			say("Freecam: ON");
		}
	}
	
	public void toggleFullbright() {
		fullbright = !fullbright;
		if(fullbright) {
			savedGamma = getGamma();
			setGamma(FULLBRIGHT_GAMMA);
		} else {
			setGamma(savedGamma);
		}
		say("Fullbright: " + (fullbright ? "ON" : "OFF"));
	}

	public void clientTickStart() {
		if(!gammaChecked) {
			gammaChecked = true;
			if(getGamma() > 1.0F) setGamma(1.0F);
		}
		
		if(fullbright) maintainFullbright();
		
		if(freecam) {
			if(!freecamValid()) {
				freecam = false;
				freecamDisable();
				say("Freecam: OFF");
				return;
			}
			freecamTickStart();
		}
	}

	public void clientTickEnd() {
		if(freecam) freecamTickEnd();
	}

	public void renderTick() {
		if(fullbright) maintainFullbright();
		if(freecam) freecamRenderTick();
	}
	
	private void maintainFullbright() {
		float g = getGamma();
		if(g != FULLBRIGHT_GAMMA) {
			savedGamma = g;
			setGamma(FULLBRIGHT_GAMMA);
		}
	}
}
