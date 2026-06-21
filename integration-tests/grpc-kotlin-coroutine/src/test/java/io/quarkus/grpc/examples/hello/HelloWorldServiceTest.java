package io.quarkus.grpc.examples.hello;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HelloWorldServiceTest {

    private Channel channel;

    @BeforeEach
    public void init() {
        channel = ManagedChannelBuilder.forAddress("localhost", 8081).usePlaintext().build();
    }

    @AfterEach
    public void cleanup() {
        GRPCTestUtils.close(channel);
    }

    @Test
    public void testCoroutineServiceHasVertxContext() {
        GreeterGrpc.GreeterBlockingStub client = GreeterGrpc.newBlockingStub(channel);
        HelloReply reply = client.sayHello(HelloRequest.newBuilder().setName("kotlin").build());

        assertThat(reply.getMessage()).isEqualTo("Hello kotlin");
        assertThat(reply.getHasVertxContext()).isTrue();
        assertThat(reply.getIsDuplicatedContext()).isTrue();
    }
}
