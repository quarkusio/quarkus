package io.quarkus.deployment.util;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.IOException;
import java.io.InputStream;

public class IoUtil {

    /**
     * Returns an input stream for reading the specified resource from the specified ClassLoader.
     * This might return {@code null}, in the case there is no matching resource.
     *
     * @param classLoader
     * @param className
     * @return
     */
    public static InputStream readClass(ClassLoader classLoader, String className) {
        return classLoader.getResourceAsStream(fromClassNameToResourceName(className));
    }

    /**
     * Returns an byte array representing the content of the specified resource as loaded
     * from the specified ClassLoader.
     * This might return {@code null}, in the case there is no matching resource.
     *
     * @param classLoader
     * @param className
     * @return
     */
    public static byte[] readClassAsBytes(ClassLoader classLoader, String className) throws IOException {
        try (InputStream stream = readClass(classLoader, className)) {
            if (stream == null) {
                return null;
            } else {
                return readBytes(stream);
            }
        }
    }

    public static byte[] readBytes(InputStream is) throws IOException {
        return is.readAllBytes();
    }

}
