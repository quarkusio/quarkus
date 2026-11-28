package io.quarkus.datasource.deployment.devservices;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.datasource.common.devservices.VolumeMount;
import io.quarkus.datasource.runtime.DevServicesBuildTimeConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithName;

public class DevServicesVolumesTest {

    /**
     * The two forms of {@code quarkus.datasource.devservices.volumes} as they are declared in
     * {@link DevServicesBuildTimeConfig}.
     */
    @ConfigMapping(prefix = "quarkus.datasource.devservices")
    interface VolumesConfig {
        Map<String, String> volumes();

        @WithName("volumes")
        Map<String, DevServicesBuildTimeConfig.Volume> namedVolumes();
    }

    @Test
    void legacyFileSystemVolumeIsAReadWriteBindMount() {
        assertThat(mounts(Map.of("quarkus.datasource.devservices.volumes.\"/host/data\"", "/container/data")))
                .containsExactly(VolumeMount.bind("/host/data", "/container/data", false));
    }

    @Test
    void legacyClasspathVolumeIsAReadOnlyClasspathMount() {
        assertThat(mounts(Map.of("quarkus.datasource.devservices.volumes.\"classpath:init.sql\"", "/container/init.sql")))
                .containsExactly(VolumeMount.classpath("init.sql", "/container/init.sql", true));
    }

    @Test
    void namedVolumesAreConverted() {
        List<VolumeMount> mounts = mounts(Map.of(
                "quarkus.datasource.devservices.volumes.\"data\".source", "/host/data",
                "quarkus.datasource.devservices.volumes.\"data\".target", "/container/data",
                "quarkus.datasource.devservices.volumes.\"init\".type", "classpath",
                "quarkus.datasource.devservices.volumes.\"init\".source", "init.sql",
                "quarkus.datasource.devservices.volumes.\"init\".target", "/container/init.sql",
                "quarkus.datasource.devservices.volumes.\"init\".read-only", "false"));

        assertThat(mounts).containsExactlyInAnyOrder(
                VolumeMount.bind("/host/data", "/container/data", false),
                VolumeMount.classpath("init.sql", "/container/init.sql", false));
    }

    @Test
    void readOnlyDefaultsToTrueForClasspathVolumesAndFalseForBindVolumes() {
        List<VolumeMount> mounts = mounts(Map.of(
                "quarkus.datasource.devservices.volumes.data.source", "/host/data",
                "quarkus.datasource.devservices.volumes.data.target", "/container/data",
                "quarkus.datasource.devservices.volumes.init.type", "classpath",
                "quarkus.datasource.devservices.volumes.init.source", "init.sql",
                "quarkus.datasource.devservices.volumes.init.target", "/container/init.sql"));

        assertThat(mounts).containsExactlyInAnyOrder(
                VolumeMount.bind("/host/data", "/container/data", false),
                VolumeMount.classpath("init.sql", "/container/init.sql", true));
    }

    @Test
    void namedVolumePropertiesCollectedByTheLegacyMapAreNotHostLocations() {
        VolumesConfig config = config(Map.of(
                "quarkus.datasource.devservices.volumes.\"/host/data\"", "/container/data",
                "quarkus.datasource.devservices.volumes.\"init\".type", "classpath",
                "quarkus.datasource.devservices.volumes.\"init\".source", "init.sql",
                "quarkus.datasource.devservices.volumes.\"init\".target", "/container/init.sql",
                "quarkus.datasource.devservices.volumes.config.source", "/host/config",
                "quarkus.datasource.devservices.volumes.config.target", "/container/config"));

        // a Map<String, String> collects every property under its prefix, whatever the number of remaining segments
        assertThat(config.volumes()).containsOnlyKeys("/host/data", "\"init\".type", "\"init\".source", "\"init\".target",
                "config.source", "config.target");
        assertThat(config.namedVolumes()).containsOnlyKeys("init", "config");

        assertThat(DevServicesVolumes.toVolumeMounts(config.volumes(), config.namedVolumes())).containsExactlyInAnyOrder(
                VolumeMount.bind("/host/data", "/container/data", false),
                VolumeMount.classpath("init.sql", "/container/init.sql", true),
                VolumeMount.bind("/host/config", "/container/config", false));
    }

    @Test
    void legacyKeysThatOnlyLookLikeVolumePropertiesAreKept() {
        List<VolumeMount> mounts = mounts(Map.of(
                "quarkus.datasource.devservices.volumes.\"/host/other.source\"", "/container/other",
                "quarkus.datasource.devservices.volumes.\"init\".type", "classpath",
                "quarkus.datasource.devservices.volumes.\"init\".source", "init.sql",
                "quarkus.datasource.devservices.volumes.\"init\".target", "/container/init.sql"));

        assertThat(mounts).contains(VolumeMount.bind("/host/other.source", "/container/other", false));
    }

    private static List<VolumeMount> mounts(Map<String, String> properties) {
        VolumesConfig config = config(properties);
        return DevServicesVolumes.toVolumeMounts(config.volumes(), config.namedVolumes());
    }

    private static VolumesConfig config(Map<String, String> properties) {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder().withMapping(VolumesConfig.class);
        properties.forEach(builder::withDefaultValue);
        SmallRyeConfig config = builder.build();
        return config.getConfigMapping(VolumesConfig.class);
    }
}
