package io.quarkus.websockets.next.test.telemetry.endpoints.onerror;

import java.lang.reflect.Type;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import io.quarkus.websockets.next.BinaryMessageCodec;
import io.vertx.core.buffer.Buffer;

@Priority(15)
@Singleton
public class DtoBinaryCodec
        implements BinaryMessageCodec<Dto> {
    @Override
    public boolean supports(Type type) {
        return type.equals(Dto.class);
    }

    @Override
    public Buffer encode(Dto dto) {
        return Buffer.buffer(dto.property());
    }

    @Override
    public Dto decode(Type type, Buffer value) {
        throw new RuntimeException("Expected exception during decoding");
    }
}
