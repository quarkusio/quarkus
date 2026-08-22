package io.quarkus.devservices.common;

import java.util.List;
import java.util.Map;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.datasource.common.devservices.VolumeMount;

public final class Volumes {

    private static final String CLASSPATH = "classpath:";
    private static final String EMPTY = "";

    private Volumes() {

    }

    public static void addVolumes(GenericContainer<?> container, Map<String, String> volumes) {
        for (Map.Entry<String, String> volume : volumes.entrySet()) {
            String hostLocation = volume.getKey();
            if (volume.getKey().startsWith(CLASSPATH)) {
                addMount(container, VolumeMount.classpath(hostLocation.replaceFirst(CLASSPATH, EMPTY), volume.getValue(),
                        true));
            } else {
                addMount(container, VolumeMount.bind(hostLocation, volume.getValue(), false));
            }
        }
    }

    public static void addMounts(GenericContainer<?> container, List<VolumeMount> mounts) {
        for (VolumeMount mount : mounts) {
            addMount(container, mount);
        }
    }

    private static void addMount(GenericContainer<?> container, VolumeMount mount) {
        BindMode bindMode = mount.readOnly() ? BindMode.READ_ONLY : BindMode.READ_WRITE;
        switch (mount.type()) {
            case CLASSPATH:
                container.withClasspathResourceMapping(mount.source(), mount.target(), bindMode);
                break;
            case BIND:
                container.withFileSystemBind(mount.source(), mount.target(), bindMode);
                break;
        }
    }
}
