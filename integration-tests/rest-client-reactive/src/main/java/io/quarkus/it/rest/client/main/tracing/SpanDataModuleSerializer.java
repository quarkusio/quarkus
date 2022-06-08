package io.quarkus.it.rest.client.main.tracing;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.jackson.JsonMapperCustomizer;

@Singleton
public class SpanDataModuleSerializer implements JsonMapperCustomizer {
    @Override
    public void customize(JsonMapper.Builder builder) {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(SpanData.class, new SpanDataSerializer());
        builder.addModule(simpleModule);
    }
}
