package io.quarkus.smallrye.reactivemessaging.rabbitmq.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "rabbitmq", phase = ConfigPhase.BUILD_TIME)
public class RabbitMQBuildTimeConfig {

    /**
     * Dev Services.
     * <p>
     * DevServices allows Quarkus to start a RabbitMQ broker in dev and test mode automatically.
     */
    @ConfigItem
    @ConfigDocSection(generated = true)
    public RabbitMQDevServicesBuildTimeConfig devservices;

    /**
     * The credentials provider name.
     */
    @ConfigItem
    public Optional<String> credentialsProvider = Optional.empty();

    /**
     * The credentials provider bean name.
     * <p>
     * This is a bean name (as in {@code @Named}) of a bean that implements {@code CredentialsProvider}.
     * It is used to select the credentials provider bean when multiple exist.
     * This is unnecessary when there is only one credentials provider available.
     * <p>
     * For Vault, the credentials provider bean name is {@code vault-credentials-provider}.
     */
    @ConfigItem
    public Optional<String> credentialsProviderName = Optional.empty();
}
