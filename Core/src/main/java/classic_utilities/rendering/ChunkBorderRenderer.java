package classic_utilities.rendering;

public abstract class ChunkBorderRenderer {
    private final ChunkBorderMesh chunkBorderMesh;

    protected ChunkBorderRenderer(ChunkBorderMesh chunkBorderMesh) {
        this.chunkBorderMesh = chunkBorderMesh;
    }

    public final void render(float partialTicks) {
        if(!prepare(partialTicks)) return;

        chunkBorderMesh.ensure(cameraBlockX() >> 4, cameraBlockZ() >> 4);

        begin();
        try {
            drawLines(chunkBorderMesh.corners(), 1.0F, 0.0F, 0.0F, 0.5F, 1.0F);
            drawLines(chunkBorderMesh.grid(), 1.0F, 1.0F, 0.0F, 1.0F, 1.0F);
            drawLines(chunkBorderMesh.sections(), 0.25F, 0.25F, 1.0F, 1.0F, 2.0F);
        } finally {
            end();
        }
    }

    /** Captures the interpolated camera for this frame. Returns false if there is no camera to draw from. */
    protected abstract boolean prepare(float partialTicks);

    /** Block x of the camera (floored), valid after {@link #prepare}. */
    protected abstract int cameraBlockX();

    /** Block z of the camera (floored), valid after {@link #prepare}. */
    protected abstract int cameraBlockZ();

    /** Makes world coordinates draw correctly relative to the camera and sets up GL state for lines. */
    protected abstract void begin();

    /** Draws line segments from a flat x,y,z,x,y,z,... array. */
    protected abstract void drawLines(double[] xyz, float r, float g, float b, float a, float width);

    /** Restores whatever {@link #begin} changed. */
    protected abstract void end();
}
