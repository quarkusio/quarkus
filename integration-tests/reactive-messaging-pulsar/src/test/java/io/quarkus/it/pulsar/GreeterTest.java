package io.quarkus.it.pulsar;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import examples.GpReply;
import examples.GpRequest;
import examples.GreeterGrpc;
import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTestResource(value = PulsarResource.class, restrictToAnnotatedClass = true)
@QuarkusTest
public class GreeterTest {

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
    public void testProto4() {
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
        GpRequest request = GpRequest.newBuilder().setName("Pulsar").build();
        GpReply reply = stub.greet(request);
        Assertions.assertEquals("Hello Pulsar", reply.getMessage());
        Assertions.assertEquals("com.google.protobuf.GeneratedMessage", reply.getClass().getSuperclass().getName());
    }
}
