package io.quarkus.grpc.examples.hello;

import org.junit.jupiter.api.Disabled;

import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;

@QuarkusIntegrationTest
@TestProfile(VertxGRPCTestProfile.class)
@Disabled("quarkus.http.ssl.client-auth is set to 'required' but it is build time fixed to 'NONE'. " +
        "Did you change the property quarkus.http.ssl.client-auth after building the application?" +
        "" +
        "How to get around this? ... As this would be a good / needed requirement for Vert.x based gRPC native test?")
class VertxHelloWorldMutualTlsEndpointIT extends VertxHelloWorldMutualTlsEndpointTestBase {

    @Override
    Vertx vertx() {
        return Vertx.vertx();
    }

    @Override
    void close(Vertx vertx) {
        GRPCTestUtils.close(vertx);
    }
}
