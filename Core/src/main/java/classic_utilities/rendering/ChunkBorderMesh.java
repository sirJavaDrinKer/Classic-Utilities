package classic_utilities.rendering;

public final class ChunkBorderMesh {
    // I'm removing all of my hair follicles
    public static final int WORLD_HEIGHT = 256;

    private static final int GRID_STEP = 2;
    private static final int SECTION_STEP = 16;

    private boolean built;
    private int chunkX;
    private int chunkZ;

    private double[] corners = new double[0];
    private double[] grid = new double[0];
    private double[] sections = new double[0];

    public void ensure(int chunkX, int chunkZ) {
        if(built && this.chunkX == chunkX && this.chunkZ == chunkZ) return;

        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        build();
        built = true;
    }

    public double[] corners() {
        return corners;
    }

    public double[] grid() {
        return grid;
    }

    public double[] sections() {
        return sections;
    }

    private void build() {
        double minX = chunkX * 16.0D;
        double minZ = chunkZ * 16.0D;
        double maxX = minX + 16.0D;
        double maxZ = minZ + 16.0D;
        double top = WORLD_HEIGHT;

        Lines out = new Lines(4 * 4);
        for(int i = -16; i <= 32; i += 16) {
            for(int j = -16; j <= 32; j += 16) {
                out.segment(minX + i, 0.0D, minZ + j, minX + i, top, minZ + j);
            }
        }
        corners = out.data;

        int gridLevels = WORLD_HEIGHT / GRID_STEP + 1;
        out = new Lines(7 * 4 + gridLevels * 4);
        for(int k = 2; k < 16; k += 2) {
            out.segment(minX + k, 0.0D, minZ, minX + k, top, minZ);
            out.segment(minX + k, 0.0D, maxZ, minX + k, top, maxZ);
            out.segment(minX, 0.0D, minZ + k, minX, top, minZ + k);
            out.segment(maxX, 0.0D, minZ + k, maxX, top, minZ + k);
        }
        for(int y = 0; y <= WORLD_HEIGHT; y += GRID_STEP) {
            out.outline(minX, y, minZ, maxX, maxZ);
        }
        grid = out.data;

        int sectionLevels = WORLD_HEIGHT / SECTION_STEP + 1;
        out = new Lines(4 + sectionLevels * 4);
        out.segment(minX, 0.0D, minZ, minX, top, minZ);
        out.segment(maxX, 0.0D, minZ, maxX, top, minZ);
        out.segment(minX, 0.0D, maxZ, minX, top, maxZ);
        out.segment(maxX, 0.0D, maxZ, maxX, top, maxZ);
        for(int y = 0; y <= WORLD_HEIGHT; y += SECTION_STEP) {
            out.outline(minX, y, minZ, maxX, maxZ);
        }
        sections = out.data;
    }

    private static final class Lines {
        final double[] data;
        int n;

        Lines(int segments) {
            data = new double[segments * 6];
        }

        void segment(double x0, double y0, double z0, double x1, double y1, double z1) {
            data[n++] = x0;
            data[n++] = y0;
            data[n++] = z0;
            data[n++] = x1;
            data[n++] = y1;
            data[n++] = z1;
        }

        void outline(double x0, double y, double z0, double x1, double z1) {
            segment(x0, y, z0, x1, y, z0);
            segment(x1, y, z0, x1, y, z1);
            segment(x1, y, z1, x0, y, z1);
            segment(x0, y, z1, x0, y, z0);
        }
    }
}
