package classic_utilities.forge.skin;

import java.awt.image.BufferedImage;

public final class ModernSkinConverter {
    public static final int SIZE = 64;

    private ModernSkinConverter() {
    }

    public static BufferedImage toModern(BufferedImage src) {
        if(src == null) return null;

        int rows = normalizedHeight(src);
        int[] px = new int[SIZE * SIZE];
        readNormalized(src, rows, px);

        boolean legacy = rows < SIZE;
        if(legacy) {
            upgradeLegacy(px);
            // The hat layer of an old skin that has no transparency at all is really just a flat
            // box around the head; modern Minecraft drops it, so do we.
            if(!hasTransparency(px, 32, 0, 64, 32)) clear(px, 32, 0, 64, 32);
        }

        opaque(px, 0, 0, 32, 16);
        opaque(px, 0, 16, 64, 32);
        opaque(px, 16, 48, 48, 64);

        BufferedImage result = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, SIZE, SIZE, px, 0, SIZE);
        return result;
    }

    private static void upgradeLegacy(int[] px) {
        // Left leg (origin 16,48) from right leg (origin 0,16). The side faces swap places.
        copyRect(px, 4, 16, 16, 32, 4, 4);   // top
        copyRect(px, 8, 16, 16, 32, 4, 4);   // bottom
        copyRect(px, 0, 20, 24, 32, 4, 12);  // right side -> left side
        copyRect(px, 4, 20, 16, 32, 4, 12);  // front
        copyRect(px, 8, 20, 8, 32, 4, 12);   // left side -> right side
        copyRect(px, 12, 20, 16, 32, 4, 12); // back

        // Left arm (origin 32,48) from right arm (origin 40,16).
        copyRect(px, 44, 16, -8, 32, 4, 4);  // top
        copyRect(px, 48, 16, -8, 32, 4, 4);  // bottom
        copyRect(px, 40, 20, 8, 32, 4, 12);  // right side -> left side
        copyRect(px, 44, 20, 0, 32, 4, 12);  // front
        copyRect(px, 48, 20, -8, 32, 4, 12); // left side -> right side
        copyRect(px, 52, 20, 0, 32, 4, 12);  // back
    }

    private static void copyRect(int[] px, int x, int y, int xOffset, int yOffset, int w, int h) {
        for(int j = 0; j < h; j++) {
            for(int i = 0; i < w; i++) {
                px[(y + yOffset + j) * SIZE + x + xOffset + i] = px[(y + j) * SIZE + x + w - 1 - i];
            }
        }
    }

    private static boolean hasTransparency(int[] px, int x0, int y0, int x1, int y1) {
        for(int y = y0; y < y1; y++) {
            for(int x = x0; x < x1; x++) {
                if((px[y * SIZE + x] >>> 24) < 128) return true;
            }
        }
        return false;
    }

    private static void clear(int[] px, int x0, int y0, int x1, int y1) {
        for(int y = y0; y < y1; y++) {
            for(int x = x0; x < x1; x++) {
                px[y * SIZE + x] &= 0x00FFFFFF;
            }
        }
    }

    private static void opaque(int[] px, int x0, int y0, int x1, int y1) {
        for(int y = y0; y < y1; y++) {
            for(int x = x0; x < x1; x++) {
                px[y * SIZE + x] |= 0xFF000000;
            }
        }
    }

    private static int normalizedHeight(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int rows = w == SIZE ? h : (int) Math.round(h * (double) SIZE / w);
        return Math.max(1, Math.min(SIZE, rows));
    }

    private static void readNormalized(BufferedImage src, int rows, int[] out) {
        int w = src.getWidth();
        int h = src.getHeight();
        if(w == SIZE) {
            src.getRGB(0, 0, SIZE, Math.min(rows, h), out, 0, SIZE);
            return;
        }
        for(int y = 0; y < rows; y++) {
            int sy = Math.min(h - 1, y * h / rows);
            for(int x = 0; x < SIZE; x++) {
                out[y * SIZE + x] = src.getRGB(Math.min(w - 1, x * w / SIZE), sy);
            }
        }
    }
}
