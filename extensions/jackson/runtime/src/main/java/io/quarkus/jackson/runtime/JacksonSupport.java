package io.quarkus.jackson.runtime;

import java.util.Optional;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public interface JacksonSupport {

    Optional<PropertyNamingStrategies.NamingBase> configuredNamingStrategy();
}
