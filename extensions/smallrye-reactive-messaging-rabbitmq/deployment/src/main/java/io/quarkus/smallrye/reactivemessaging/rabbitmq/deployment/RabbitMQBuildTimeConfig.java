package io.quarkus.smallrye.reactivemessaging.rabbitmq.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "rabbitmq", phase = ConfigPhase.BUILD_TIME)
public class RabbitMQBuildTimeConfig {

    /**
     * Configuration for DevServices. DevServices allows Quarkus to automatically start a RabbitMQ broker in dev and test mode.
     */
    @ConfigItem
    public RabbitMQDevServicesBuildTimeConfig devservices;

    /**
     * The credentials provider name.
     */
    @ConfigItem
    public Optional<String> credentialsProvider = Optional.empty();

    /**
     * The credentials provider bean name.
     * <p>
     * It is the {@code &#64;Named} value of the credentials provider bean. It is used to discriminate if multiple
     * CredentialsProvider beans are available.
     * <p>
     * For Vault it is: vault-credentials-provider. Not necessary if there is only one credentials provider available.
     */
    @ConfigItem
    public Optional<String> credentialsProviderName = Optional.empty();
}
