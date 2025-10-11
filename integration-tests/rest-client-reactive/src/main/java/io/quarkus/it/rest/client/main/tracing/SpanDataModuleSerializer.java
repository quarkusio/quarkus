package io.quarkus.it.rest.client.main.tracing;

import jakarta.inject.Singleton;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class SpanDataModuleSerializer implements ObjectMapperCustomizer {
    @Override
    public void customize(ObjectMapper objectMapper) {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(SpanData.class, new SpanDataSerializer());
        objectMapper.registerModule(simpleModule);
    }
}
