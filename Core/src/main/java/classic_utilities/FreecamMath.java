package classic_utilities;

public final class FreecamMath {
    /*      Where does a mathematician go when they're hurt? L'Hospital.
    *       *Audience Laughter*
    */
    private FreecamMath() {}
	public static double[] velocity(float yawDegrees, float pitchDegrees, double forward, double strafe, double vertical, double speed) {

        double yaw = Math.toRadians(yawDegrees);
		double pitch = Math.toRadians(pitchDegrees);
		double cosPitch = Math.cos(pitch);

		double dx = -Math.sin(yaw) * cosPitch * forward + Math.cos(yaw) * strafe;
		double dy = -Math.sin(pitch) * forward + vertical;
		double dz = Math.cos(yaw) * cosPitch * forward + Math.sin(yaw) * strafe;
		
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if(len < 1.0E-4D) return new double[] { 0, 0, 0 };

		double scale = speed / len;
		return new double[] { dx * scale, dy * scale, dz * scale };
	}
}
