package io.quarkus.it.opentracing.json;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.opentracing.mock.MockSpan;
import io.quarkus.jackson.JsonMapperCustomizer;

@Singleton
public class MockSpanModuleSerializer implements JsonMapperCustomizer {
    @Override
    public void customize(JsonMapper.Builder builder) {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(MockSpan.class, new MockSpanSerializer());
        builder.addModule(simpleModule);
    }
}
