package io.quarkus.smallrye.faulttolerance.runtime;

import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import io.smallrye.faulttolerance.ExistingCircuitBreakerNames;

@Singleton
@Alternative
@Priority(1)
public class QuarkusExistingCircuitBreakerNames implements ExistingCircuitBreakerNames {
    private Set<String> names;

    void init(Set<String> names) {
        this.names = names;
    }

    @Override
    public boolean contains(String name) {
        return names.contains(name);
    }
}
