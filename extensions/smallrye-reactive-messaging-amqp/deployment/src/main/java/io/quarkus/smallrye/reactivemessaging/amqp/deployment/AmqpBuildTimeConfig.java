package io.quarkus.smallrye.reactivemessaging.amqp.deployment;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.amqp")
public interface AmqpBuildTimeConfig {

    /**
     * Dev Services.
     * <p>
     * Dev Services allows Quarkus to automatically start an AMQP broker in dev and test mode.
     */
    @ConfigDocSection(generated = true)
    AmqpDevServicesBuildTimeConfig devservices();
}
