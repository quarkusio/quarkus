package io.quarkus.consul.config.runtime;

import io.smallrye.mutiny.Uni;

interface ConsulConfigGateway {

    /**
     * Retrieves a value from Consul's Key / Value store using the value of {@code key}
     */
    Uni<Response> getValue(String key);

    void close();
}
