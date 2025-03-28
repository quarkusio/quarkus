package io.quarkus.deployment.dev.devservices;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.compose")
public interface ComposeBuildTimeConfig {

    /**
     * Compose dev services config
     */
    @ConfigDocSection(generated = true)
    ComposeDevServicesBuildTimeConfig devservices();
}
