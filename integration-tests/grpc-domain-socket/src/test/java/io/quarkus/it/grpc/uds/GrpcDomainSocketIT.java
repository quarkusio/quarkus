package io.quarkus.it.grpc.uds;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.grpc.Channel;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpcio.client.GrpcIoClient;
import io.vertx.grpcio.client.GrpcIoClientChannel;

@QuarkusIntegrationTest
@WithTestResource(GrpcDomainSocketTestResource.class)
@DisabledOnOs(OS.WINDOWS)
public class GrpcDomainSocketIT {

    private Vertx vertx;
    private GrpcIoClient client;
    private Channel channel;

    @BeforeEach
    public void setUp() {
        String socketPath = ConfigProvider.getConfig()
                .getValue("quarkus.http.domain-socket", String.class);
        vertx = Vertx.vertx();
        client = GrpcIoClient.client(vertx);
        // gRPC over HTTP/2 requires the :authority pseudo-header, which Vert.x derives from
        // SocketAddress.host(). Domain socket addresses return null, so we wrap with "localhost".
        SocketAddress address = new DomainSocketAddressWithHost(socketPath);
        channel = new GrpcIoClientChannel(client, address);
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            client.close().toCompletionStage().toCompletableFuture().join();
        }
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }

    @Test
    public void testGrpcOverDomainSocket() {
        HelloReply reply = MutinyGreeterGrpc.newMutinyStub(channel)
                .sayHello(HelloRequest.newBuilder().setName("World").build())
                .await().atMost(Duration.ofSeconds(10));
        assertThat(reply.getMessage()).isEqualTo("Hello World");
    }

    private record DomainSocketAddressWithHost(String path) implements SocketAddress {

        @Override
        public boolean isDomainSocket() {
            return true;
        }

        @Override
        public String host() {
            return "localhost";
        }

        @Override
        public String hostName() {
            return "localhost";
        }

        @Override
        public int port() {
            return 0;
        }

        @Override
        public boolean isInetSocket() {
            return false;
        }

        @Override
        public String hostAddress() {
            return null;
        }

        @Override
        public String toString() {
            return path;
        }
    }
}
