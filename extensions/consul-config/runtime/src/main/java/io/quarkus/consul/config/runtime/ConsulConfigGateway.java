package io.quarkus.consul.config.runtime;

import java.io.IOException;
import java.util.Optional;

interface ConsulConfigGateway {

    /**
     * Retrieves a value from Consul's Key / Value store using the value of {@code key}
     */
    Optional<Response> getValue(String key) throws IOException;
}
