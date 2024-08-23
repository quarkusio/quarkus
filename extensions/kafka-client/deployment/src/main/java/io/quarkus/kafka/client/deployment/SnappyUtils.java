package io.quarkus.kafka.client.deployment;

import org.xerial.snappy.OSInfo;

/**
 * This class should only be used if Snappy is available on the classpath.
 */
public class SnappyUtils {

    private SnappyUtils() {
        // Avoid direct instantiation
    }

    public static String getNativeLibFolderPathForCurrentOS() {
        return OSInfo.getNativeLibFolderPathForCurrentOS();
    }
}
