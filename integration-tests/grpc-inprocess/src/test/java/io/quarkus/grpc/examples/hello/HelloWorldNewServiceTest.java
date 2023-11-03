package io.quarkus.grpc.examples.hello;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HelloWorldNewServiceTest {

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub stub;

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub mutiny;

    @Test
    public void testHelloWorldServiceUsingBlockingStub() {
        HelloReply reply = stub.sayHello(HelloRequest.newBuilder().setName("neo-blocking").build());
        Assertions.assertEquals(reply.getMessage(), "Hello neo-blocking");
    }

    @Test
    public void testHelloWorldServiceUsingMutinyStub() {
        HelloReply reply = mutiny
                .sayHello(HelloRequest.newBuilder().setName("neo-blocking").build())
                .await().atMost(Duration.ofSeconds(5));
        Assertions.assertEquals(reply.getMessage(), "Hello neo-blocking");
    }

}
