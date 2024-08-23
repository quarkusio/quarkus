package com.example.grpc.exc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;

public class SmallryeHelloGrpcServiceTestBase {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @GrpcClient
    HelloGrpc hello;

    @Test
    public void testHello() {
        final StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> hello
                .sayHello(HelloRequest.newBuilder().setName("Neo").build()).await()
                .atMost(Duration.ofSeconds(5)));
        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
    }

}
