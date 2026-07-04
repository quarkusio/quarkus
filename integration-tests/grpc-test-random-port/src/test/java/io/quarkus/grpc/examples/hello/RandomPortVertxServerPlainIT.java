package io.quarkus.grpc.examples.hello;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import examples.GreeterGrpc;
import examples.HelloRequest;
import io.quarkus.grpc.runtime.GrpcServer;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpcio.client.GrpcIoClient;
import io.vertx.grpcio.client.GrpcIoClientChannel;

@QuarkusIntegrationTest
@TestProfile(RandomPortVertxServerPlainTestBase.Profile.class)
class RandomPortVertxServerPlainIT extends RandomPortVertxServerPlainTestBase {
    GrpcServer grpcServer;

    @Test
    void testWithNative() {
        int serverPort = grpcServer.getPort();
        Vertx vertx = Vertx.vertx();
        GrpcIoClient client = GrpcIoClient.client(vertx);
        GrpcIoClientChannel channel = new GrpcIoClientChannel(client,
                SocketAddress.inetSocketAddress(serverPort, "localhost"));
        try {
            var stub = GreeterGrpc.newBlockingStub(channel);
            HelloRequest request = HelloRequest.newBuilder().setName("neo").build();
            var resp = stub.sayHello(request);
            assertThat(resp.getMessage()).startsWith("Hello neo");

            assertThat(serverPort).isNotEqualTo(0);
        } finally {
            client.close().toCompletionStage().toCompletableFuture().join();
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }
}
