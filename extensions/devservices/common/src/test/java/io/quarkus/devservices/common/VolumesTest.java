package io.quarkus.devservices.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;

import io.quarkus.datasource.common.devservices.VolumeMount;

public class VolumesTest {

    private static final DockerImageName IMAGE = DockerImageName.parse("alpine:3");

    @Test
    void bindMountCanBeReadWrite() {
        GenericContainer<?> container = new GenericContainer<>(IMAGE);

        Volumes.addVolumes(container, List.of(VolumeMount.bind("/host/data", "/container/data", false)));

        assertThat(container.getBinds()).hasSize(1);
        Bind bind = container.getBinds().get(0);
        assertThat(bind.getPath()).isEqualTo("/host/data");
        assertThat(bind.getVolume().getPath()).isEqualTo("/container/data");
        assertThat(bind.getAccessMode()).isEqualTo(AccessMode.rw);
    }

    @Test
    void bindMountCanBeReadOnly() {
        GenericContainer<?> container = new GenericContainer<>(IMAGE);

        Volumes.addVolumes(container, List.of(VolumeMount.bind("/host/config", "/container/config", true)));

        assertThat(container.getBinds()).hasSize(1);
        Bind bind = container.getBinds().get(0);
        assertThat(bind.getAccessMode()).isEqualTo(AccessMode.ro);
    }

    @Test
    void readOnlyClasspathMountIsCopiedIntoTheContainer() {
        GenericContainer<?> container = new GenericContainer<>(IMAGE);

        Volumes.addVolumes(container, List.of(VolumeMount.classpath("mount-test/init.sql", "/container/init.sql", true)));

        // Testcontainers implements read-only classpath mappings as file copies, not binds
        assertThat(container.getBinds()).isEmpty();
        assertThat(container.getCopyToFileContainerPathMap()).containsValue("/container/init.sql");
    }

    @Test
    void readWriteClasspathMountIsBoundFromTheResolvedResourcePath() {
        GenericContainer<?> container = new GenericContainer<>(IMAGE);

        Volumes.addVolumes(container, List.of(VolumeMount.classpath("mount-test/init.sql", "/container/init.sql", false)));

        String resolvedPath = MountableFile.forClasspathResource("mount-test/init.sql").getResolvedPath();
        assertThat(container.getBinds()).hasSize(1);
        Bind bind = container.getBinds().get(0);
        // compare as paths, not strings: on Windows the recorded bind uses backslashes
        assertThat(Path.of(bind.getPath())).isEqualTo(Path.of(resolvedPath));
        assertThat(bind.getVolume().getPath()).isEqualTo("/container/init.sql");
        assertThat(bind.getAccessMode()).isEqualTo(AccessMode.rw);
    }
}
