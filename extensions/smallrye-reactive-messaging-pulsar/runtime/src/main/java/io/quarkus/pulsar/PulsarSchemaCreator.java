package io.quarkus.pulsar;

import org.apache.pulsar.client.api.Schema;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.pulsar.schema.BufferSchema;
import io.quarkus.pulsar.schema.JsonArraySchema;
import io.quarkus.pulsar.schema.JsonObjectSchema;
import io.quarkus.pulsar.schema.ObjectMapperSchema;

/**
 * Creates the {@link Schema} beans generated for the types found on Pulsar channels.
 */
public class PulsarSchemaCreator implements BeanCreator<Object> {

    public static final String KIND = "kind";
    public static final String TYPE = "type";

    public enum Kind {
        JSON,
        AVRO,
        PROTOBUF,
        OBJECT_MAPPER,
        BUFFER,
        JSON_OBJECT,
        JSON_ARRAY,
        BYTE_BUFFER,
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object create(SyntheticCreationalContext<Object> context) {
        Kind kind = (Kind) context.getParams().get(KIND);
        return switch (kind) {
            case JSON -> Schema.JSON(type(context));
            case AVRO -> Schema.AVRO(type(context));
            case PROTOBUF -> Schema.PROTOBUF((Class) type(context));
            case OBJECT_MAPPER -> ObjectMapperSchema.of(type(context));
            case BUFFER -> BufferSchema.INSTANCE;
            case JSON_OBJECT -> JsonObjectSchema.INSTANCE;
            case JSON_ARRAY -> JsonArraySchema.INSTANCE;
            case BYTE_BUFFER -> Schema.BYTEBUFFER;
        };
    }

    private static Class<?> type(SyntheticCreationalContext<Object> context) {
        String name = (String) context.getParams().get(TYPE);
        try {
            return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load the Pulsar schema type " + name, e);
        }
    }
}
