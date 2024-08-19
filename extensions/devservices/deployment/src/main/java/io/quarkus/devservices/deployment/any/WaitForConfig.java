package io.quarkus.devservices.deployment.any;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface WaitForConfig {

    /**
     * defaultWaitStrategy
     */
    Optional<Boolean> defaultWaitStrategy();

    /**
     * forHealthcheck
     */
    Optional<Boolean> healthcheck();

    /**
     * forHttp
     */
    Optional<String> http();

    /**
     * forHttps
     */
    Optional<String> https();

    /**
     * forListeningPort
     */
    Optional<Boolean> listeningPort();

    /**
     * forListeningPorts
     */
    Optional<Integer[]> listeningPorts();

    /**
     * forLogMessage
     */
    Optional<String> logMessage();

    /**
     * forLogMessage times
     */
    @WithDefault("1")
    int logMessageTimes();

    /**
     * forSuccessfulCommand
     */
    Optional<String> successfulCommand();

}
