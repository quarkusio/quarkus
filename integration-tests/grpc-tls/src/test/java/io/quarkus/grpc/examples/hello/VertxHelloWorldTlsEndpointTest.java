package io.quarkus.grpc.examples.hello;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(VertxGRPCTestProfile.class)
class VertxHelloWorldTlsEndpointTest extends HelloWorldTlsEndpointTestBase {

}
