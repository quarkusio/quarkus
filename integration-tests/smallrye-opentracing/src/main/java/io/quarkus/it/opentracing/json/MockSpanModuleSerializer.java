package io.quarkus.it.opentracing.json;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.opentracing.mock.MockSpan;
import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class MockSpanModuleSerializer implements ObjectMapperCustomizer {
    @Override
    public void customize(ObjectMapper objectMapper) {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(MockSpan.class, new MockSpanSerializer());
        objectMapper.registerModule(simpleModule);
    }
}
