package classic_utilities;

public final class SpawnScan {
    public interface Query {
        boolean canSpawn(int x, int y, int z);
    }

    private int[] positions = new int[3 * 1024];
    private int count;

    public void clear() {
        count = 0;
    }

    public int size() {
        return count;
    }

    public int x(int i) {
        return positions[i * 3];
    }

    public int y(int i) {
        return positions[i * 3 + 1];
    }

    public int z(int i) {
        return positions[i * 3 + 2];
    }

    public void scan(Query query, int centerX, int centerY, int centerZ, int radius, int verticalRadius) {
        count = 0;

        int minY = Math.max(1, centerY - verticalRadius);
        int maxY = Math.min(254, centerY + verticalRadius); //y + 1 has to exist too
        int radiusSq = radius * radius;

        for(int x = centerX - radius; x <= centerX + radius; x++) {
            for(int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                if(dx * dx + dz * dz > radiusSq) continue;

                for(int y = minY; y <= maxY; y++) {
                    if(query.canSpawn(x, y, z)) add(x, y, z);
                }
            }
        }
    }

    private void add(int x, int y, int z) {
        if(count * 3 + 3 > positions.length) {
            int[] bigger = new int[positions.length * 2];
            System.arraycopy(positions, 0, bigger, 0, count * 3);
            positions = bigger;
        }
        positions[count * 3] = x;
        positions[count * 3 + 1] = y;
        positions[count * 3 + 2] = z;
        count++;
    }
}