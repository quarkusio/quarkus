package io.quarkus.grpc.examples.hello;

import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.grpc.Channel;
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

}
