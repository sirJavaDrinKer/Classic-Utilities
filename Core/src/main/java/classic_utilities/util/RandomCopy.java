package classic_utilities.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

public final class RandomCopy {
    private RandomCopy() {
    }

    public static Random copy(Random source) {
        if(source == null) return null;

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(160);
            ObjectOutputStream out = new ObjectOutputStream(bytes);
            try {
                out.writeObject(source);
            } finally {
                out.close();
            }

            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
            try {
                return (Random) in.readObject();
            } finally {
                in.close();
            }
        } catch(IOException e) {
            return null;
        } catch(ClassNotFoundException e) {
            return null;
        } catch(ClassCastException e) {
            return null;
        }
    }

    public static long stateHash(Random source) {
        Random copy = copy(source);
        return copy == null ? 0L : copy.nextLong();
    }
}
