package io.quarkus.grpc.examples.hello;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(VertxGRPCTestProfile.class)
class VertxHelloWorldTlsEndpointIT extends HelloWorldTlsEndpointTestBase {
}
