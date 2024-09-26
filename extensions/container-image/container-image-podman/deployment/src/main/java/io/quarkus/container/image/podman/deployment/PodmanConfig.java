package io.quarkus.container.image.podman.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.container.image.docker.common.deployment.CommonConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.podman")
public interface PodmanConfig extends CommonConfig {
    /**
     * Which platform(s) to target during the build. See
     * https://docs.podman.io/en/latest/markdown/podman-build.1.html#platform-os-arch-variant
     */
    Optional<List<String>> platform();

    /**
     * Require HTTPS and verify certificates when contacting registries
     */
    @WithDefault("true")
    boolean tlsVerify();
}
