package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import java.lang.reflect.Type;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import io.quarkus.websockets.next.TextMessageCodec;

@Priority(15) // this must have higher priority than JsonCodec or tests will be flaky
@Singleton
public class DtoTextCodec implements TextMessageCodec<Dto> {
    @Override
    public boolean supports(Type type) {
        return type.equals(Dto.class);
    }

    @Override
    public String encode(Dto dto) {
        return dto.property();
    }

    @Override
    public Dto decode(Type type, String value) {
        return new Dto(value);
    }
}
