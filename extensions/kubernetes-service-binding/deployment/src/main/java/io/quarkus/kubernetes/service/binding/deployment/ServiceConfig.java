package io.quarkus.kubernetes.service.binding.deployment;

import java.util.Optional;

public interface ServiceConfig {

    /**
     * The name of the service binding.
     * If no value is specified the id of the service will be used instead.
     */
    Optional<String> binding();

    /**
     * The kind of the service.
     */
    Optional<String> kind();

    /**
     * The apiVersion of the service
     */
    Optional<String> apiVersion();

    /**
     * The name of the service.
     * When this is empty the key of the service is meant to be used as name.
     */
    Optional<String> name();

    /**
     * The namespace of the service.
     */
    Optional<String> namespace();
}
