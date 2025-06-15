package io.quarkus.pulsar.schema;

import org.apache.pulsar.client.impl.schema.AbstractSchema;
import org.apache.pulsar.client.impl.schema.SchemaInfoImpl;
import org.apache.pulsar.common.schema.SchemaInfo;
import org.apache.pulsar.common.schema.SchemaType;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;

public class BufferSchema extends AbstractSchema<Buffer> {

    public static final BufferSchema INSTANCE = new BufferSchema();

    private static final SchemaInfo SCHEMA_INFO = SchemaInfoImpl.builder().name("Buffer").type(SchemaType.BYTES)
            .schema(new byte[0]).build();

    @Override
    public Buffer decode(ByteBuf byteBuf) {
        if (byteBuf == null)
            return null;

        return Buffer.buffer(byteBuf);
    }

    @Override
    public byte[] encode(Buffer message) {
        if (message == null)
            return null;

        return message.getBytes();
    }

    @Override
    public SchemaInfo getSchemaInfo() {
        return SCHEMA_INFO;
    }
}
