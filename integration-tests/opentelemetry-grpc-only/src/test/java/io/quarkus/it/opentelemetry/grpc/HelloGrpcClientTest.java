package io.quarkus.it.opentelemetry.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.acme.HelloGrpc;
import org.acme.HelloReply;
import org.acme.HelloRequest;
import org.junit.jupiter.api.Test;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.timeouts.TestTimeouts;

@QuarkusTest
public class HelloGrpcClientTest {
    @GrpcClient
    HelloGrpc helloGrpc;

    @Test
    void testHello() {
        HelloReply reply = helloGrpc
                .sayHello(HelloRequest.newBuilder().setName("Neo").build()).await().atMost(TestTimeouts.get());
        assertEquals("Hello Neo!", reply.getMessage());
    }
}
