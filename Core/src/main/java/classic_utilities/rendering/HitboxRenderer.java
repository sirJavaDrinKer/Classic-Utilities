package classic_utilities.rendering;

public abstract class HitboxRenderer {
    public static final double MAX_DISTANCE = 96.0D;
    public static final float LINE_WIDTH = 2.0F;

    private static final double EYE_BOX_HALF_HEIGHT = 0.01D;
    private static final double LOOK_LENGTH = 2.0D;

    private final LineBuffer boxes = new LineBuffer();
    private final LineBuffer eyes = new LineBuffer();
    private final LineBuffer looks = new LineBuffer();
    private final HitboxTarget target = new HitboxTarget();

    public final void render(float partialTicks) {
        if(!prepare(partialTicks)) return;

        boxes.clear();
        eyes.clear();
        looks.clear();

        int count = entityCount();
        for(int i = 0; i < count; i++) {
            if(!fill(i, partialTicks, target)) continue;

            HitboxTarget t = target;
            boxes.box(t.minX, t.minY, t.minZ, t.maxX, t.maxY, t.maxZ);

            if(t.living) {
                eyes.box(t.minX, t.eyeY - EYE_BOX_HALF_HEIGHT, t.minZ, t.maxX, t.eyeY + EYE_BOX_HALF_HEIGHT, t.maxZ);
            }

            double cx = (t.minX + t.maxX) * 0.5D;
            double cz = (t.minZ + t.maxZ) * 0.5D;
            looks.segment(cx, t.eyeY, cz,
                    cx + t.lookX * LOOK_LENGTH, t.eyeY + t.lookY * LOOK_LENGTH, cz + t.lookZ * LOOK_LENGTH);
        }

        if(boxes.isEmpty()) return;

        begin();
        try {
            drawLines(boxes.toArray(), 1.0F, 1.0F, 1.0F, 1.0F, LINE_WIDTH);
            if(!eyes.isEmpty()) drawLines(eyes.toArray(), 1.0F, 0.0F, 0.0F, 1.0F, LINE_WIDTH);
            drawLines(looks.toArray(), 0.0F, 0.0F, 1.0F, 1.0F, LINE_WIDTH);
        } finally {
            end();
        }
    }

    protected abstract boolean prepare(float partialTicks);

    protected abstract int entityCount();

    protected abstract boolean fill(int index, float partialTicks, HitboxTarget out);

    protected abstract void begin();

    protected abstract void drawLines(double[] xyz, float r, float g, float b, float a, float width);

    protected abstract void end();
}
