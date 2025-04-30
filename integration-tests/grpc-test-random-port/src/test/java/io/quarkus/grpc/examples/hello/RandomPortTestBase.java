package io.quarkus.grpc.examples.hello;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.google.common.net.HostAndPort;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc.MutinyGreeterStub;
import io.grpc.Channel;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.DisabledOnIntegrationTest;

abstract class RandomPortTestBase {
    @GrpcClient("hello")
    MutinyGreeterStub client;

    @GrpcClient("hello")
    Channel channel;

    @Test
    @DisabledOnIntegrationTest
    void testRandomPort() {
        assertSoftly(softly -> {
            HelloRequest request = HelloRequest.newBuilder().setName("neo").build();
            HelloReply reply = client.sayHello(request).await().indefinitely();
            softly.assertThat(reply.getMessage()).startsWith("Hello neo");

            int clientPort = HostAndPort.fromString(channel.authority()).getPort();
            int serverPort = ConfigProvider.getConfig().getValue(serverPortProperty(), Integer.class);
            softly.assertThat(clientPort).isNotEqualTo(0);
            softly.assertThat(clientPort).isEqualTo(serverPort);
        });
    }

    protected abstract String serverPortProperty();
}
