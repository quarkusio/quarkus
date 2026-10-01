package io.quarkus.test.opentelemetry;

import java.util.Map;

import jakarta.inject.Singleton;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * OTel SDK 1.65 changed AttributesMap so it no longer extends HashMap. As a result the default
 * (reflection based) JSON serialization of a SpanData's attributes no longer emits the attribute
 * entries, only bean getters like "empty" and "totalAddedValues". This customizer renders any
 * OpenTelemetry {@link Attributes} instance as a flat {key: value} map so the exported spans keep
 * exposing their attributes to the tests.
 */
@Singleton
public class SpanAttributesJsonMapperCustomizer implements JsonMapperBuilderCustomizer {

    @Override
    public void customize(JsonMapper.Builder builder) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Attributes.class, new ValueSerializer<Attributes>() {
            @Override
            public void serialize(Attributes attributes, JsonGenerator gen, SerializationContext ctxt) {
                gen.writeStartObject();
                for (Map.Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
                    gen.writePOJOProperty(entry.getKey().getKey(), entry.getValue());
                }
                gen.writeEndObject();
            }
        });
        builder.addModule(module);
    }
}
