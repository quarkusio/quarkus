package com.example.grpc.exc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.timeouts.TestTimeouts;

public class SmallryeHelloGrpcServiceTestBase {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @GrpcClient
    HelloGrpc hello;

    @Test
    public void testHello() {
        final StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> hello
                .sayHello(HelloRequest.newBuilder().setName("Neo").build()).await()
                .atMost(TestTimeouts.get()));
        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
    }

}
