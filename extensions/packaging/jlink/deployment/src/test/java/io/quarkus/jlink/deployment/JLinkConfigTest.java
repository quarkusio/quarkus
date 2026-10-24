package io.quarkus.jlink.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.pkg.PackageConfig;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;

class JLinkConfigTest {

    @Test
    void launcherNameDefaultsToBuildBaseName() {
        assertThat(config(Map.of("quarkus.build.base-name", "shop-1.0.0")).launcherName()).isEqualTo("shop-1.0.0");
    }

    @Test
    void launcherNameFollowsPackageOutputName() {
        assertThat(config(Map.of("quarkus.package.output-name", "shop")).launcherName()).isEqualTo("shop");
    }

    @Test
    void explicitLauncherName() {
        assertThat(config(Map.of("quarkus.package.output-name", "shop", "quarkus.jlink.launcher-name", "run"))
                .launcherName()).isEqualTo("run");
    }

    @Test
    void outputDirectoryWithoutPackageOutputDirectory() {
        assertThat(config(Map.of()).outputDirectory()).isEqualTo(Path.of(""));
    }

    @Test
    void outputDirectoryFollowsPackageOutputDirectory() {
        assertThat(config(Map.of("quarkus.package.output-directory", "quarkus-app")).outputDirectory())
                .isEqualTo(Path.of("quarkus-app"));
    }

    @Test
    void explicitOutputDirectory() {
        assertThat(config(Map.of("quarkus.package.output-directory", "quarkus-app",
                "quarkus.jlink.output-directory", "out/jlink")).outputDirectory()).isEqualTo(Path.of("out/jlink"));
    }

    private static JLinkConfig config(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addSystemSources()
                .withMapping(PackageConfig.class)
                .withMapping(JLinkConfig.class)
                .withInterceptors(new JLinkConfigDefaultsInterceptor())
                .withSources(new PropertiesConfigSource(properties, "test", 100))
                .build()
                .getConfigMapping(JLinkConfig.class);
    }
}
