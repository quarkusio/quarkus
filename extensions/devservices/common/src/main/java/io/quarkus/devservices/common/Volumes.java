package io.quarkus.devservices.common;

import java.util.Map;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

public final class Volumes {

    private static final String CLASSPATH = "classpath:";
    private static final String EMPTY = "";

    private Volumes() {

    }

    public static void addVolumes(GenericContainer<?> container, Map<String, String> volumes) {
        for (Map.Entry<String, String> volume : volumes.entrySet()) {
            String hostLocation = volume.getKey();
            if (volume.getKey().startsWith(CLASSPATH)) {
                container.withClasspathResourceMapping(hostLocation.replaceFirst(CLASSPATH, EMPTY), volume.getValue(),
                        BindMode.READ_ONLY);
            } else {
                container.withFileSystemBind(hostLocation, volume.getValue(), BindMode.READ_WRITE);
            }
        }
    }
}
