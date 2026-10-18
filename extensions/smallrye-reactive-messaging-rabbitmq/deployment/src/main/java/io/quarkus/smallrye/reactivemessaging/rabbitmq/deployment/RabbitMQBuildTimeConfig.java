package io.quarkus.smallrye.reactivemessaging.rabbitmq.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.rabbitmq")
public interface RabbitMQBuildTimeConfig {

    /**
     * Dev Services.
     * <p>
     * DevServices allows Quarkus to start a RabbitMQ broker in dev and test mode automatically.
     */
    @ConfigDocSection(generated = true)
    RabbitMQDevServicesBuildTimeConfig devservices();

    /**
     * The credentials provider name.
     */
    Optional<String> credentialsProvider();

    /**
     * The credentials provider bean name.
     * <p>
     * This is a bean name (as in {@code @Named}) of a bean that implements {@code CredentialsProvider}.
     * It is used to select the credentials provider bean when multiple exist.
     * This is unnecessary when there is only one credentials provider available.
     * <p>
     * For Vault, the credentials provider bean name is {@code vault-credentials-provider}.
     */
    Optional<String> credentialsProviderName();
}
