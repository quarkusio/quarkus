package io.quarkus.smallrye.reactivemessaging.amqp.deployment;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "amqp", phase = ConfigPhase.BUILD_TIME)
public class AmqpBuildTimeConfig {

    /**
     * Dev Services.
     * <p>
     * Dev Services allows Quarkus to automatically start an AMQP broker in dev and test mode.
     */
    @ConfigItem
    @ConfigDocSection(generated = true)
    public AmqpDevServicesBuildTimeConfig devservices;
}
