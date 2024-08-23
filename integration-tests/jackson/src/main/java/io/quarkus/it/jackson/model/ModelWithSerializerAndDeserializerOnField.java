package io.quarkus.it.jackson.model;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(ignoreNested = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelWithSerializerAndDeserializerOnField {

    private String name;

    @JsonDeserialize(using = InnerDeserializer.class)
    @JsonSerialize(using = InnerSerializer.class)
    private Inner inner;

    public ModelWithSerializerAndDeserializerOnField() {
    }

    public ModelWithSerializerAndDeserializerOnField(String name, Inner inner) {
        this.name = name;
        this.inner = inner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Inner getInner() {
        return inner;
    }

    public void setInner(Inner inner) {
        this.inner = inner;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Inner {
        private String someValue;

        public Inner() {
        }

        public Inner(String someValue) {
            this.someValue = someValue;
        }

        public String getSomeValue() {
            return someValue;
        }

        public void setSomeValue(String someValue) {
            this.someValue = someValue;
        }
    }

    public static class InnerDeserializer extends JsonDeserializer<Inner> {

        @Override
        public Inner deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return new Inner("immutable");
        }
    }

    public static class InnerSerializer extends JsonSerializer<Inner> {
        @Override
        public void serialize(Inner value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("someValue", "unchangeable");
            gen.writeEndObject();
        }
    }

}
