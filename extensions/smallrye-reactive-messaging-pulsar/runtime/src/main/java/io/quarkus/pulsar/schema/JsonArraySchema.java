package io.quarkus.pulsar.schema;

import org.apache.pulsar.client.impl.schema.AbstractSchema;
import org.apache.pulsar.client.impl.schema.SchemaInfoImpl;
import org.apache.pulsar.common.schema.SchemaInfo;
import org.apache.pulsar.common.schema.SchemaType;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;

public class JsonArraySchema extends AbstractSchema<JsonArray> {

    public static final JsonArraySchema INSTANCE = new JsonArraySchema();

    private static final SchemaInfo SCHEMA_INFO = SchemaInfoImpl.builder().name("JsonArray").type(SchemaType.NONE)
            .schema(new byte[0]).build();

    @Override
    public JsonArray decode(ByteBuf byteBuf) {
        if (byteBuf == null)
            return null;

        return Buffer.buffer(byteBuf).toJsonArray();
    }

    @Override
    public byte[] encode(JsonArray message) {
        if (message == null)
            return null;

        return message.encode().getBytes();
    }

    @Override
    public SchemaInfo getSchemaInfo() {
        return SCHEMA_INFO;
    }
}
