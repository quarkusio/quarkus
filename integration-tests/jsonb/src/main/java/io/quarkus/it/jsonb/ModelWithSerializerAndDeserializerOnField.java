package io.quarkus.it.jsonb;

import java.lang.reflect.Type;

import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.annotation.JsonbTypeSerializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ModelWithSerializerAndDeserializerOnField {

    private String name;

    @JsonbTypeDeserializer(InnerDeserializer.class)
    @JsonbTypeSerializer(InnerSerializer.class)
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

    public static class InnerDeserializer implements JsonbDeserializer<Inner> {

        @Override
        public Inner deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            return new Inner("immutable");
        }
    }

    public static class InnerSerializer implements JsonbSerializer<Inner> {

        @Override
        public void serialize(Inner obj, JsonGenerator gen, SerializationContext ctx) {
            gen.writeStartObject();
            gen.write("someValue", "unchangeable");
            gen.writeEnd();
        }
    }

}
