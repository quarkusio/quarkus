package io.quarkus.it.pulsar;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import examples.HelloGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTestResource(value = PulsarResource.class, restrictToAnnotatedClass = true)
@QuarkusTest
public class HelloWorldTest {

    private Channel channel;

    @BeforeEach
    public void init() {
        channel = GRPCTestUtils.channel(null, getPort());
    }

    public int getPort() {
        return ConfigProvider.getConfig().getValue("quarkus.grpc.server.test-port", Integer.class);
    }

    @AfterEach
    public void cleanup() {
        GRPCTestUtils.close(channel);
    }

    @Test
    public void testProto3() {
        HelloGrpc.HelloBlockingStub stub = HelloGrpc.newBlockingStub(channel);
        HelloRequest request = HelloRequest.newBuilder().setName("Pulsar").build();
        HelloReply reply = stub.sayHello(request);
        Assertions.assertEquals("Hello World, Pulsar", reply.getMessage());
        Assertions.assertEquals("com.google.protobuf.GeneratedMessageV3", reply.getClass().getSuperclass().getName());
    }
}
