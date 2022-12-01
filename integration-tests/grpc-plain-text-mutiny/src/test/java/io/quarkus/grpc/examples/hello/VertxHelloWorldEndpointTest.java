package io.quarkus.grpc.examples.hello;

import org.junit.jupiter.api.Disabled;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
@Disabled("Enable once load balancing / Stork is ready")
class VertxHelloWorldEndpointTest extends HelloWorldEndpointTestBase {
}
