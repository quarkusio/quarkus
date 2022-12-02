package io.quarkus.kafka.client.serialization;

import org.apache.kafka.common.serialization.Serdes;

import io.vertx.core.buffer.Buffer;

public final class BufferSerde extends Serdes.WrapperSerde<Buffer> {

    public BufferSerde() {
        super(new BufferSerializer(), new BufferDeserializer());
    }

}
