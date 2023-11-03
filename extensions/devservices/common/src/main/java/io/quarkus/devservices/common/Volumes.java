package io.quarkus.devservices.common;

import java.net.URL;
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
            BindMode bindMode = BindMode.READ_WRITE;
            if (volume.getKey().startsWith(CLASSPATH)) {
                URL url = Thread.currentThread().getContextClassLoader()
                        .getResource(hostLocation.replaceFirst(CLASSPATH, EMPTY));
                if (url == null) {
                    throw new IllegalStateException("Classpath resource at '" + hostLocation + "' not found!");
                }

                hostLocation = url.getPath();
                bindMode = BindMode.READ_ONLY;
            }

            container.withFileSystemBind(hostLocation, volume.getValue(), bindMode);
        }
    }
}
