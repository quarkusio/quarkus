package io.quarkus.it.opentelemetry.scheduler;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class SpanDataModuleSerializer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(SpanData.class, new SpanDataSerializer());
        objectMapper.registerModule(module);
    }
}
