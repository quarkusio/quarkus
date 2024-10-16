package io.quarkus.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.netty.NettyChannelBuilder;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;

public class GrpcServerInboundMessageWithSeparateServerTest {

    static String configuration = """
            quarkus.grpc.server.max-inbound-message-size=512000
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClass(HelloService.class)
                    .add(new StringAsset(configuration), "application.properties"));

    protected ManagedChannel channel;

    @BeforeEach
    public void init() throws Exception {
        channel = NettyChannelBuilder.forAddress("localhost", 9001)
                .usePlaintext()
                .build();
    }

    @AfterEach
    public void shutdown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @Test
    public void testInvokingWithPayloadUnderLimit() {
        var sizeInChars = 400 * 1024;
        HelloRequest request = HelloRequest.newBuilder().setName("a".repeat(sizeInChars)).build();

        HelloReply reply = GreeterGrpc.newBlockingStub(channel)
                .sayHello(request);
        assertThat(reply).isNotNull();
    }

    @Test
    public void testInvokingWithPayloadAboveLimit() {
        var sizeInChars = 1000 * 1024;
        HelloRequest request = HelloRequest.newBuilder().setName("a".repeat(sizeInChars)).build();

        assertThatThrownBy(() -> GreeterGrpc.newBlockingStub(channel).sayHello(request))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("RESOURCE_EXHAUSTED");
    }
}
