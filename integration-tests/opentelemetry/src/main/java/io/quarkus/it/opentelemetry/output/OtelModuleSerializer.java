package io.quarkus.it.opentelemetry.output;

import jakarta.inject.Singleton;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;

import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class OtelModuleSerializer implements ObjectMapperCustomizer {
    @Override
    public void customize(ObjectMapper objectMapper) {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(SpanData.class, new SpanDataSerializer());
        simpleModule.addSerializer(LogRecordData.class, new LogRecordDataSerializer());
        objectMapper.registerModule(simpleModule);
    }
}
