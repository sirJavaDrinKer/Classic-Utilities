package classic_utilities;

public class HudMessage {
    private String text;
    private long startMs;
    private long holdMs;
    private long fadeMs;

    /**
     * @param text string to display, or null to clear
     * @param holdMs milliseconds to hold the message fully opaque before beginning to fade out
     * @param fadeMs milliseconds to fade the message out.
     */

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