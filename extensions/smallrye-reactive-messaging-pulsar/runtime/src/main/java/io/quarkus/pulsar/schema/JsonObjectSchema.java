package io.quarkus.pulsar.schema;

import org.apache.pulsar.client.impl.schema.AbstractSchema;
import org.apache.pulsar.client.impl.schema.SchemaInfoImpl;
import org.apache.pulsar.common.schema.SchemaInfo;
import org.apache.pulsar.common.schema.SchemaType;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class JsonObjectSchema extends AbstractSchema<JsonObject> {

    public static final JsonObjectSchema INSTANCE = new JsonObjectSchema();

    private static final SchemaInfo SCHEMA_INFO = SchemaInfoImpl.builder().name("JsonObject").type(SchemaType.NONE)
            .schema(new byte[0]).build();

    @Override
    public JsonObject decode(ByteBuf byteBuf) {
        if (byteBuf == null)
            return null;
        Buffer buffer = Buffer.buffer(byteBuf);
        return buffer.toJsonObject();
    }

    @Override
    public byte[] encode(JsonObject message) {
        if (message == null)
            return null;

        return message.encode().getBytes();
    }

    @Override
    public SchemaInfo getSchemaInfo() {
        return SCHEMA_INFO;
    }
}
