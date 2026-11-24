package classic_utilities.rendering;

public class HudMessage {

    private String text;
    private long startMs;
    private long holdMs;
    private long fadeMs;

    public void show(String text, long holdMs, long fadeMs) {
        this.text = text;
        this.startMs = now();
        this.holdMs = Math.max(0L, holdMs);
        this.fadeMs = Math.max(0L, fadeMs);
    }

    public void clear() {
        text = null;
    }

    public String getText() {
        return text;
    }

    public float getOpacity() {
        if(text == null) return 0.0F;

        long age = now() - startMs;
        if(age < holdMs) return 1.0F;

        long intoFade = age - holdMs;
        if(intoFade >= fadeMs) return 0.0F;

        return 1.0F - (float) intoFade / (float) fadeMs;
    }

    private static long now() {
        return System.nanoTime() / 1000000L;
    }
}