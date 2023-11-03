package io.quarkus.grpc.examples.hello;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;
import io.vertx.grpc.client.GrpcClient;

@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
@Disabled("quarkus.http.ssl.client-auth is set to 'required' but it is build time fixed to 'NONE'. " +
        "Did you change the property quarkus.http.ssl.client-auth after building the application?" +
        "" +
        "How to get around this? ... As this would be a good / needed requirement for Vert.x based gRPC native test?")
class VertxHelloWorldMutualTlsServiceIT extends HelloWorldMutualTlsServiceTestBase {

    Vertx vertx;

    GrpcClient client;

    @BeforeEach
    public void init() throws Exception {
        vertx = Vertx.vertx();
        Map.Entry<GrpcClient, Channel> pair = GRPCTestUtils.tls(vertx, "tls/ca.pem", "tls/client.pem", "tls/client.key");
        client = pair.getKey();
        channel = pair.getValue();
    }

    @AfterEach
    public void cleanup() {
        GRPCTestUtils.close(client);
        GRPCTestUtils.close(vertx);
    }

}
