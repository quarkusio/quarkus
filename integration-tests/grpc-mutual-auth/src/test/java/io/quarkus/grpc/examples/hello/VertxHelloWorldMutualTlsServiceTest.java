package io.quarkus.grpc.examples.hello;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;
import io.vertx.grpc.client.GrpcClient;

@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
class VertxHelloWorldMutualTlsServiceTest extends HelloWorldMutualTlsServiceTestBase {

    @Inject
    Vertx vertx;

    GrpcClient client;

    @BeforeEach
    public void init() throws Exception {
        Map.Entry<GrpcClient, Channel> pair = GRPCTestUtils.tls(vertx, "tls/ca.pem", "tls/client.pem", "tls/client.key");
        client = pair.getKey();
        channel = pair.getValue();
    }

    @AfterEach
    public void cleanup() {
        GRPCTestUtils.close(client);
    }

    @Test
    public void testRolesHelloWorldServiceUsingBlockingStub() {
        GreeterGrpc.GreeterBlockingStub client = GreeterGrpc.newBlockingStub(channel);
        HelloReply reply = client
                .sayHelloRoleUser(HelloRequest.newBuilder().setName("neo-blocking").build());
        assertThat(reply.getMessage())
                .isEqualTo("Hello neo-blocking from CN=testclient,O=Default Company Ltd,L=Default City,C=XX");
        assertThrows(StatusRuntimeException.class,
                () -> client.sayHelloRoleAdmin(HelloRequest.newBuilder().setName("neo-blocking").build()));
    }

    @Test
    public void testRolesHelloWorldServiceUsingMutinyStub() {
        HelloReply reply = MutinyGreeterGrpc.newMutinyStub(channel)
                .sayHelloRoleUser(HelloRequest.newBuilder().setName("neo-blocking").build())
                .await().atMost(Duration.ofSeconds(5));
        assertThat(reply.getMessage())
                .isEqualTo("Hello neo-blocking from CN=testclient,O=Default Company Ltd,L=Default City,C=XX");
        assertThrows(StatusRuntimeException.class, () -> MutinyGreeterGrpc.newMutinyStub(channel)
                .sayHelloRoleAdmin(HelloRequest.newBuilder().setName("neo-blocking").build())
                .await().atMost(Duration.ofSeconds(5)));
    }
}
