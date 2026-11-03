package classic_utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class EnchantProtocol {
    public static final String CHANNEL = "cu_enchant";

    private static final byte TYPE_PREVIEW = 1;

    public static final int MAX_ENCHANTS_PER_SLOT = 32;

    private EnchantProtocol() {
    }

    public static byte[] encode(EnchantPreview preview) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(64);
        DataOutputStream out = new DataOutputStream(bytes);

        out.writeByte(TYPE_PREVIEW);
        out.writeInt(preview.windowId);

        for(int slot = 0; slot < EnchantPreview.SLOTS; slot++) {
            int count = Math.min(preview.ids[slot].length, MAX_ENCHANTS_PER_SLOT);

            out.writeInt(preview.costs[slot]);
            out.writeByte(count);
            for(int i = 0; i < count; i++) {
                out.writeShort(preview.ids[slot][i]);
                out.writeShort(preview.strengths[slot][i]);
            }
        }

        return bytes.toByteArray();
    }

    public static EnchantPreview decode(byte[] data) throws IOException {
        if(data == null || data.length == 0 || data[0] != TYPE_PREVIEW) return null;

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        in.readByte();

        int windowId = in.readInt();
        int[] costs = new int[EnchantPreview.SLOTS];
        int[][] ids = new int[EnchantPreview.SLOTS][];
        int[][] strengths = new int[EnchantPreview.SLOTS][];

        for(int slot = 0; slot < EnchantPreview.SLOTS; slot++) {
            costs[slot] = in.readInt();
            if(costs[slot] < 0) throw new IOException("Bad cost: " + costs[slot]);

            int count = in.readByte();
            if(count < 0 || count > MAX_ENCHANTS_PER_SLOT) throw new IOException("Bad enchantment count: " + count);

            ids[slot] = new int[count];
            strengths[slot] = new int[count];
            for(int i = 0; i < count; i++) {
                ids[slot][i] = in.readShort();
                strengths[slot][i] = in.readShort();
            }
        }

        return new EnchantPreview(windowId, costs, ids, strengths);
    }
}
