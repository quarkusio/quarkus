package io.quarkus.pulsar.schema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.pulsar.client.impl.schema.AbstractSchema;
import org.apache.pulsar.client.impl.schema.SchemaInfoImpl;
import org.apache.pulsar.common.schema.SchemaInfo;
import org.apache.pulsar.common.schema.SchemaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

public class ObjectMapperSchema<T> extends AbstractSchema<T> {

    private static final SchemaInfo SCHEMA_INFO = SchemaInfoImpl.builder().name("ObjectMapper").type(SchemaType.NONE)
            .schema(new byte[0]).build();

    public static <T> ObjectMapperSchema<T> of(Class<T> type) {
        return new ObjectMapperSchema<>(type);
    }

    private final ObjectMapper objectMapper;
    private final JavaType javaType;
    private final boolean nullAsNull;

    public ObjectMapperSchema(Class<T> type) {
        this(type, ObjectMapperProducer.get());
    }

    public ObjectMapperSchema(Class<T> type, boolean nullAsNull) {
        this(type, ObjectMapperProducer.get(), nullAsNull);
    }

    public ObjectMapperSchema(Class<T> type, ObjectMapper objectMapper) {
        this(TypeFactory.defaultInstance().constructType(type), objectMapper, false);
    }

    public ObjectMapperSchema(Class<T> type, ObjectMapper objectMapper, boolean nullAsNull) {
        this(TypeFactory.defaultInstance().constructType(type), objectMapper, nullAsNull);
    }

    public ObjectMapperSchema(TypeReference<T> typeReference, ObjectMapper objectMapper) {
        this(TypeFactory.defaultInstance().constructType(typeReference), objectMapper, false);
    }

    public ObjectMapperSchema(TypeReference<T> typeReference, ObjectMapper objectMapper, boolean nullAsNull) {
        this(TypeFactory.defaultInstance().constructType(typeReference), objectMapper, nullAsNull);
    }

    public ObjectMapperSchema(JavaType javaType, ObjectMapper objectMapper, boolean nullAsNull) {
        this.javaType = javaType;
        this.objectMapper = objectMapper;
        this.nullAsNull = nullAsNull;
    }

    @Override
    public T decode(ByteBuf byteBuf) {
        if (byteBuf == null) {
            return null;
        }

        try (InputStream is = new ByteBufInputStream(byteBuf)) {
            return objectMapper.readValue(is, javaType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] encode(T message) {
        if (nullAsNull && message == null) {
            return null;
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            objectMapper.writeValue(output, message);
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SchemaInfo getSchemaInfo() {
        return SCHEMA_INFO;
    }
}
