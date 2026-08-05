package org.acme.serialization;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;

/**
 * Mimics the Oracle {@code CharacterConverterOGS} pattern where resource access
 * and ObjectInputStream usage are split across inner classes:
 * <ul>
 *   <li>{@code ResourceLocator} calls {@code Class.getResource()} to find the resource</li>
 *   <li>{@code ObjectReader} wraps the stream in {@code ObjectInputStream} and deserializes</li>
 * </ul>
 * This tests that the tree shaker detects the pattern even when the two signals
 * are in different classes within the same dependency.
 */
public class ResourceDeserializer {

    /**
     * Locates the serialized resource on the classpath.
     */
    static class ResourceLocator {
        static URL locate() {
            return ResourceDeserializer.class.getResource("data.ser");
        }
    }

    /**
     * Reads a serialized object from a stream using ObjectInputStream.
     */
    static class ObjectReader {
        static Object read(URL url) throws Exception {
            try (InputStream is = url.openStream();
                    ObjectInputStream ois = new ObjectInputStream(is)) {
                return ois.readObject();
            }
        }
    }

    public static String load() {
        try {
            URL url = ResourceLocator.locate();
            if (url == null) {
                return "resource-not-found";
            }
            Object obj = ObjectReader.read(url);
            return obj.getClass().getSimpleName();
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
}
