package io.quarkus.grpc.examples.hello;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.net.HostAndPort;

import examples.GreeterGrpc;
import examples.HelloRequest;
import io.grpc.netty.NettyChannelBuilder;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(RandomPortSeparateServerPlainTestBase.Profile.class)
class RandomPortSeparateServerPlainIT extends RandomPortSeparateServerPlainTestBase {

    @Test
    void testWithNative() {
        var channel = NettyChannelBuilder.forAddress("localhost", 9000).usePlaintext().build();
        var stub = GreeterGrpc.newBlockingStub(channel);
        HelloRequest request = HelloRequest.newBuilder().setName("neo").build();
        var resp = stub.sayHello(request);
        assertThat(resp.getMessage()).startsWith("Hello neo");

        int clientPort = HostAndPort.fromString(channel.authority()).getPort();
        assertThat(clientPort).isNotEqualTo(0);
        assertThat(clientPort).isEqualTo(9000);

        channel.shutdownNow();
    }
}
