package io.quarkus.devservices.common;

import java.util.List;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.datasource.common.devservices.VolumeMount;

public final class Volumes {

    private Volumes() {
    }

    public static void addVolumes(GenericContainer<?> container, List<VolumeMount> volumes) {
        for (VolumeMount volume : volumes) {
            BindMode bindMode = volume.readOnly() ? BindMode.READ_ONLY : BindMode.READ_WRITE;
            switch (volume.type()) {
                case CLASSPATH:
                    container.withClasspathResourceMapping(volume.source(), volume.target(), bindMode);
                    break;
                case BIND:
                    container.withFileSystemBind(volume.source(), volume.target(), bindMode);
                    break;
            }
        }
    }
}
