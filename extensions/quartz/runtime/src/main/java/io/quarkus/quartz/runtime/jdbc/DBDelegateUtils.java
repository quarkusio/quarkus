package io.quarkus.quartz.runtime.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

class DBDelegateUtils {
    /**
     * A method to deserialize a marshalled object in an input stream.
     * This implementation uses {@link QuarkusObjectInputStream} instead of {@link ObjectInputStream} to workaround
     * a {@link ClassNotFoundException} issue observed in Test & Dev mode when `resolveClass(ObjectStreamClass)` is called.
     */
    static Object getObjectFromInput(InputStream binaryInput) throws ClassNotFoundException, IOException {
        if (binaryInput == null || binaryInput.available() == 0) {
            return null;
        }
        // use an instance of the QuarkusObjectInputStream class instead of the ObjectInputStream when deserializing
        // to workaround a CNFE in test and dev mode.
        try (ObjectInputStream in = new QuarkusObjectInputStream(binaryInput)) {
            return in.readObject();
        }
    }
}
