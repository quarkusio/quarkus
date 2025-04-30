package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.pulsar")
public interface PulsarBuildTimeConfig {

    /**
     * Dev Services.
     * <p>
     * Dev Services allows Quarkus to automatically start a Pulsar Container in dev and test mode.
     */
    @ConfigDocSection(generated = true)
    PulsarDevServicesBuildTimeConfig devservices();
}
