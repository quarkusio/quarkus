package io.quarkus.test.opentelemetry;

import java.io.IOException;
import java.util.Map;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.quarkus.jackson.ObjectMapperCustomizer;

/**
 * OTel SDK 1.65 changed AttributesMap so it no longer extends HashMap. As a result the default
 * (reflection based) JSON serialization of a SpanData's attributes no longer emits the attribute
 * entries, only bean getters like "empty" and "totalAddedValues". This customizer renders any
 * OpenTelemetry {@link Attributes} instance as a flat {key: value} map so the exported spans keep
 * exposing their attributes to the tests.
 */
@Singleton
public class SpanAttributesJsonMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Attributes.class, new JsonSerializer<Attributes>() {
            @Override
            public void serialize(Attributes attributes, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeStartObject();
                for (Map.Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
                    gen.writePOJOField(entry.getKey().getKey(), entry.getValue());
                }
                gen.writeEndObject();
            }
        });
        objectMapper.registerModule(module);
    }
}
