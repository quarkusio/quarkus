package io.quarkus.smallrye.reactivemessaging.amqp.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AmqpDevServicesBuildTimeConfig {

    /**
     * If Dev Services for AMQP has been explicitly enabled or disabled. Dev Services are generally enabled
     * by default, unless there is an existing configuration present. For AMQP, Dev Services starts a broker unless
     * {@code amqp-host} or {@code amqp-port} are set or if all the Reactive Messaging AMQP channel are configured with
     * {@code host} or {@code port}.
     */
    @ConfigItem
    public Optional<Boolean> enabled = Optional.empty();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @ConfigItem
    public Optional<Integer> port;

    /**
     * The image to use.
     * Note that only {@code quay.io/artemiscloud/activemq-artemis-broker} images are supported.
     *
     * Check https://quay.io/repository/artemiscloud/activemq-artemis-broker to find the available versions.
     */
    @ConfigItem(defaultValue = "quay.io/artemiscloud/activemq-artemis-broker:0.1.2")
    public String imageName;

    /**
     * The value of the {@code AMQ_EXTRA_ARGS} environment variable to pass to the container.
     */
    @ConfigItem(defaultValue = "--no-autotune --mapped --no-fsync")
    public String extraArgs;

}
