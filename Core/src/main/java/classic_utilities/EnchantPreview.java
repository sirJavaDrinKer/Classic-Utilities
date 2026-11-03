package classic_utilities;

public final class EnchantPreview {
    public static final int SLOTS = 3;

    public final int windowId;
    public final int[] costs;
    public final int[][] ids;
    public final int[][] strengths;

    public EnchantPreview(int windowId, int[] costs, int[][] ids, int[][] strengths) {
        if(costs.length != SLOTS || ids.length != SLOTS || strengths.length != SLOTS) {
            throw new IllegalArgumentException("Need " + SLOTS + " slots");
        }

        this.windowId = windowId;
        this.costs = costs.clone();
        this.ids = new int[SLOTS][];
        this.strengths = new int[SLOTS][];
        for(int slot = 0; slot < SLOTS; slot++) {
            if(ids[slot].length != strengths[slot].length) {
                throw new IllegalArgumentException("ids and strengths differ in length for slot " + slot);
            }
            this.ids[slot] = ids[slot].clone();
            this.strengths[slot] = strengths[slot].clone();
        }
    }
}
