package io.quarkus.it.jackson.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.quarkus.runtime.annotations.RegisterForReflection;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

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

    public static class InnerDeserializer extends ValueDeserializer<Inner> {

        @Override
        public Inner deserialize(JsonParser p, DeserializationContext ctxt) {
            return new Inner("immutable");
        }
    }

    public static class InnerSerializer extends ValueSerializer<Inner> {
        @Override
        public void serialize(Inner value, JsonGenerator gen, SerializationContext context) {
            gen.writeStartObject();
            gen.writeStringProperty("someValue", "unchangeable");
            gen.writeEndObject();
        }
    }

}
