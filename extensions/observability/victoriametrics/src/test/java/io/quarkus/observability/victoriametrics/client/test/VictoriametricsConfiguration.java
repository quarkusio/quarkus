package io.quarkus.observability.victoriametrics.client.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.observability.promql.client.util.ObservabilityObjectMapperFactory;

@ApplicationScoped
public class VictoriametricsConfiguration {
    @Singleton
    public ObjectMapper objectMapper() {
        return ObservabilityObjectMapperFactory.createObjectMapper();
    }
}
