package io.quarkus.grpc.examples.hello;

import org.junit.jupiter.api.Disabled;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(VertxGRPCTestProfile.class)
@Disabled("Enable once load balancing / Stork is ready")
class VertxHelloWorldEndpointIT extends HelloWorldEndpointTestBase {

}
