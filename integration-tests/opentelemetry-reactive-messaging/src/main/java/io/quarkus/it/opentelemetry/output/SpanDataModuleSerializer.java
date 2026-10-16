package io.quarkus.it.opentelemetry.output;

import jakarta.inject.Singleton;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@Singleton
public class SpanDataModuleSerializer implements JsonMapperBuilderCustomizer {
    @Override
    public void customize(JsonMapper.Builder builder) {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(SpanData.class, new SpanDataSerializer());
        builder.addModule(simpleModule);
    }
}
