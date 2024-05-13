package io.quarkus.it.smallrye.config;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public interface Alias extends Named {
    @JsonProperty
    Optional<String> alias();
}
