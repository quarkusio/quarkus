package io.quarkus.deployment.util;

import java.io.IOException;
import java.io.InputStream;

public class IoUtil {
    public static InputStream readClass(ClassLoader classLoader, String className) {
        return classLoader.getResourceAsStream(className.replace('.', '/') + ".class");
    }

    public static byte[] readClassAsBytes(ClassLoader classLoader, String className) throws IOException {
        try (InputStream stream = readClass(classLoader, className)) {
            return readBytes(stream);
        }
    }

    public static byte[] readBytes(InputStream is) throws IOException {
        return is.readAllBytes();
    }
}
