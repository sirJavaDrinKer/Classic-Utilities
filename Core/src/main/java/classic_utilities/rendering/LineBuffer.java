package classic_utilities.rendering;

import java.util.Arrays;

public final class LineBuffer {
    private double[] data = new double[96];
    private int size;

    public void clear() {
        size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void segment(double x0, double y0, double z0, double x1, double y1, double z1) {
        ensure(6);
        data[size++] = x0;
        data[size++] = y0;
        data[size++] = z0;
        data[size++] = x1;
        data[size++] = y1;
        data[size++] = z1;
    }

    public void box(double x0, double y0, double z0, double x1, double y1, double z1) {
        // bottom
        segment(x0, y0, z0, x1, y0, z0);
        segment(x1, y0, z0, x1, y0, z1);
        segment(x1, y0, z1, x0, y0, z1);
        segment(x0, y0, z1, x0, y0, z0);
        // top
        segment(x0, y1, z0, x1, y1, z0);
        segment(x1, y1, z0, x1, y1, z1);
        segment(x1, y1, z1, x0, y1, z1);
        segment(x0, y1, z1, x0, y1, z0);
        // verticals
        segment(x0, y0, z0, x0, y1, z0);
        segment(x1, y0, z0, x1, y1, z0);
        segment(x1, y0, z1, x1, y1, z1);
        segment(x0, y0, z1, x0, y1, z1);
    }

    public double[] toArray() {
        return Arrays.copyOf(data, size);
    }

    private void ensure(int extra) {
        if(size + extra > data.length) {
            data = Arrays.copyOf(data, Math.max(data.length * 2, size + extra));
        }
    }
}
