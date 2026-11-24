package classic_utilities.network;

import classic_utilities.options.WorldOptions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class OptionsProtocol {
    public static final String CHANNEL = "cu_options";

    public static final byte TYPE_STATE = 1;
    public static final byte TYPE_SET = 2;
    public static final byte TYPE_QUERY = 3;

    private OptionsProtocol() {
    }

    public static final class ServerState {
        public final boolean canEdit;
        public final WorldOptions options;

        public ServerState(boolean canEdit, WorldOptions options) {
            this.canEdit = canEdit;
            this.options = options;
        }
    }

    public static final class SetRequest {
        public final int id;
        public final boolean value;

        public SetRequest(int id, boolean value) {
            this.id = id;
            this.value = value;
        }
    }

    public static int type(byte[] data) {
        return data == null || data.length == 0 ? -1 : data[0];
    }

    public static byte[] encodeState(boolean canEdit, WorldOptions options) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(8);
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(TYPE_STATE);
        out.writeBoolean(canEdit);
        for(int id = 0; id < WorldOptions.COUNT; id++) {
            out.writeBoolean(options.get(id));
        }
        return bytes.toByteArray();
    }

    public static ServerState decodeState(byte[] data) throws IOException {
        if(type(data) != TYPE_STATE) return null;

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        in.readByte();
        boolean canEdit = in.readBoolean();
        boolean freecam = in.readBoolean();
        boolean preview = in.readBoolean();
        boolean griefing = in.readBoolean();
        boolean collision = in.readBoolean();
        boolean noFog = in.readBoolean();
        boolean anvilLimitRemoved = in.readBoolean();
        return new ServerState(canEdit, new WorldOptions(freecam, preview, griefing, collision, noFog, anvilLimitRemoved));
    }

    public static byte[] encodeSet(int id, boolean value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(4);
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(TYPE_SET);
        out.writeByte(id);
        out.writeBoolean(value);
        return bytes.toByteArray();
    }

    public static SetRequest decodeSet(byte[] data) throws IOException {
        if(type(data) != TYPE_SET) return null;

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        in.readByte();
        int id = in.readByte();
        boolean value = in.readBoolean();
        if(!WorldOptions.isValidId(id)) throw new IOException("Bad option id: " + id);
        return new SetRequest(id, value);
    }

    public static byte[] encodeQuery() {
        return new byte[] { TYPE_QUERY };
    }
}
