package io.quarkus.stork;

import java.util.Optional;

import io.smallrye.stork.api.ServiceInstance;

public interface QuarkusServiceInstance extends ServiceInstance {
    String getScheme();

    default Optional<String> getDomainSocket() {
        return Optional.empty();
    }
}
