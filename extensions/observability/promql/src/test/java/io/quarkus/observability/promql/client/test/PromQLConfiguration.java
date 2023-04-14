package io.quarkus.observability.promql.client.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.observability.promql.client.util.ObservabilityObjectMapperFactory;

@ApplicationScoped
public class PromQLConfiguration {
    @Singleton
    public ObjectMapper objectMapper() {
        return ObservabilityObjectMapperFactory.createObjectMapper();
    }
}
