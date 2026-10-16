package io.quarkus.jackson.runtime;

import java.util.Optional;

import tools.jackson.databind.PropertyNamingStrategies;

public interface JacksonSupport {

    Optional<PropertyNamingStrategies.NamingBase> configuredNamingStrategy();
}
