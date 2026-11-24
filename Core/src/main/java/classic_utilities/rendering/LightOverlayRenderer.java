package classic_utilities.rendering;

import classic_utilities.world.SpawnScan;

import static classic_utilities.ClassicUtilities.OVERLAY_ALPHA;

public abstract class LightOverlayRenderer {
    private static final double OVERLAY_HEIGHT = 0.01D;
    private static final double OVERLAY_INSET = 0.03D;

    private final SpawnScan spawnScan;

    protected LightOverlayRenderer(SpawnScan spawnScan) {
        this.spawnScan = spawnScan;
    }

    public final void render(float partialTicks) {
        int count = spawnScan.size();
        if(count == 0 || !prepare(partialTicks)) return;

        double vx = cameraX();
        double vy = cameraY();
        double vz = cameraZ();

        begin(1.0F, 0.0F, 0.0F, OVERLAY_ALPHA);
        for(int i = 0; i < count; i++) {
            double x0 = spawnScan.x(i) - vx + OVERLAY_INSET;
            double x1 = spawnScan.x(i) - vx + 1.0D - OVERLAY_INSET;
            double y = spawnScan.y(i) - vy + OVERLAY_HEIGHT;
            double z0 = spawnScan.z(i) - vz + OVERLAY_INSET;
            double z1 = spawnScan.z(i) - vz + 1.0D - OVERLAY_INSET;

            horizontalQuad(x0, z0, x1, z1, y);
        }
        end();
    }

    protected abstract boolean prepare(float partialTicks);

    protected abstract double cameraX();

    protected abstract double cameraY();

    protected abstract double cameraZ();

    protected abstract void begin(float r, float g, float b, float a);

    protected abstract void horizontalQuad(double x0, double z0, double x1, double z1, double y);

    protected abstract void end();
}
