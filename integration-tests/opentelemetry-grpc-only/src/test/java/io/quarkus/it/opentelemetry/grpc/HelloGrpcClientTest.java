package io.quarkus.it.opentelemetry.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.acme.HelloGrpc;
import org.acme.HelloReply;
import org.acme.HelloRequest;
import org.junit.jupiter.api.Test;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HelloGrpcClientTest {
    @GrpcClient
    HelloGrpc helloGrpc;

    @Test
    void testHello() {
        HelloReply reply = helloGrpc
                .sayHello(HelloRequest.newBuilder().setName("Neo").build()).await().atMost(Duration.ofSeconds(5));
        assertEquals("Hello Neo!", reply.getMessage());
    }
}
