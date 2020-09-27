package io.quarkus.logging.json.structured;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public interface JsonGenerator extends Closeable {

    void writeStartObject() throws IOException;

    void writeEndObject() throws IOException;

    void flush() throws IOException;

    void writeFieldName(String name) throws IOException;

    void writeObject(Object pojo) throws IOException;

    void writeObjectFieldStart(String fieldName) throws IOException;

    void writeObjectField(String fieldName, Object pojo) throws IOException;

    void writeArrayFieldStart(String fieldName) throws IOException;

    void writeEndArray() throws IOException;

    void writeString(String text) throws IOException;

    void writeStringField(String fieldName, String value) throws IOException;

    void writeNumberField(String fieldName, short value) throws IOException;

    void writeNumberField(String fieldName, int value) throws IOException;

    void writeNumberField(String fieldName, long value) throws IOException;

    void writeNumberField(String fieldName, BigInteger value) throws IOException;

    void writeNumberField(String fieldName, float value) throws IOException;

    void writeNumberField(String fieldName, double value) throws IOException;

    void writeNumberField(String fieldName, BigDecimal value) throws IOException;
}
