package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "pulsar", phase = ConfigPhase.BUILD_TIME)
public class PulsarBuildTimeConfig {

    /**
     * Dev Services.
     * <p>
     * Dev Services allows Quarkus to automatically start a Pulsar Container in dev and test mode.
     */
    @ConfigItem
    @ConfigDocSection(generated = true)
    public PulsarDevServicesBuildTimeConfig devservices;
}
