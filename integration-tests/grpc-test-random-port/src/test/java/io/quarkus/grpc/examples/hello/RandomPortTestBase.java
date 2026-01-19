package io.quarkus.grpc.examples.hello;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.Test;

import com.google.common.net.HostAndPort;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc.MutinyGreeterStub;
import io.grpc.Channel;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.runtime.GrpcServer;
import io.quarkus.test.junit.DisabledOnIntegrationTest;

abstract class RandomPortTestBase {
    @GrpcClient("hello")
    MutinyGreeterStub client;

    @GrpcClient("hello")
    Channel channel;

    @Test
    @DisabledOnIntegrationTest
    void testRandomPort(GrpcServer grpcServer) {
        assertSoftly(softly -> {
            HelloRequest request = HelloRequest.newBuilder().setName("neo").build();
            HelloReply reply = client.sayHello(request).await().indefinitely();
            softly.assertThat(reply.getMessage()).startsWith("Hello neo");

            int clientPort = HostAndPort.fromString(channel.authority()).getPort();
            int serverPort = grpcServer.getPort();
            softly.assertThat(clientPort).isNotEqualTo(0);
            softly.assertThat(clientPort).isEqualTo(serverPort);
        });
    }
}
