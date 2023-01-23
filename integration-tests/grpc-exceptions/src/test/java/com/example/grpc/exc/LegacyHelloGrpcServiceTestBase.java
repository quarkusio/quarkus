package com.example.grpc.exc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;

class LegacyHelloGrpcServiceTestBase {
    @SuppressWarnings("CdiInjectionPointsInspection")
    @GrpcClient
    LegacyHelloGrpcGrpc.LegacyHelloGrpcBlockingStub stub;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void legacySayHello() {
        final StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
                () -> stub.legacySayHello(HelloRequest.newBuilder().setName("Neo").build()));
        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
    }
}
