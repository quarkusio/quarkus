package io.quarkus.logging.json.structured.jackson;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import io.quarkus.logging.json.structured.JsonGenerator;

public class JacksonJsonGenerator implements JsonGenerator {
    private final com.fasterxml.jackson.core.JsonGenerator generator;

    JacksonJsonGenerator(com.fasterxml.jackson.core.JsonGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void writeStartObject() throws IOException {
        this.generator.writeStartObject();
    }

    @Override
    public void writeEndObject() throws IOException {
        this.generator.writeEndObject();
    }

    @Override
    public void flush() throws IOException {
        this.generator.flush();
    }

    @Override
    public void close() throws IOException {
        this.generator.close();
    }

    @Override
    public void writeFieldName(String name) throws IOException {
        this.generator.writeFieldName(name);
    }

    @Override
    public void writeObject(Object pojo) throws IOException {
        this.generator.writeObject(pojo);
    }

    @Override
    public void writeObjectFieldStart(String fieldName) throws IOException {
        this.generator.writeObjectFieldStart(fieldName);
    }

    @Override
    public void writeObjectField(String fieldName, Object pojo) throws IOException {
        this.generator.writeObjectField(fieldName, pojo);
    }

    @Override
    public void writeArrayFieldStart(String fieldName) throws IOException {
        this.generator.writeArrayFieldStart(fieldName);
    }

    @Override
    public void writeEndArray() throws IOException {
        this.generator.writeEndArray();
    }

    @Override
    public void writeString(String text) throws IOException {
        this.generator.writeString(text);
    }

    @Override
    public void writeStringField(String fieldName, String value) throws IOException {
        this.generator.writeStringField(fieldName, value);
    }

    @Override
    public void writeNumberField(String fieldName, short value) throws IOException {
        this.generator.writeNumberField(fieldName, value);
    }

    @Override
    public void writeNumberField(String fieldName, int value) throws IOException {
        this.generator.writeNumberField(fieldName, value);
    }

    @Override
    public void writeNumberField(String fieldName, long value) throws IOException {
        this.generator.writeNumberField(fieldName, value);
    }

    @Override
    public void writeNumberField(String fieldName, BigInteger value) throws IOException {
        this.generator.writeNumberField(fieldName, value);
    }

    @Override
    public void writeNumberField(String fieldName, float value) throws IOException {
        this.generator.writeNumberField(fieldName, value);
    }

    @Override
    public void writeNumberField(String fieldName, double value) throws IOException {
        this.generator.writeNumberField(fieldName, value);
    }

    @Override
    public void writeNumberField(String fieldName, BigDecimal value) throws IOException {
        this.generator.writeNumberField(fieldName, value);
    }
}
