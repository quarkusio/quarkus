package io.quarkus.grpc.example.interceptors;

import io.quarkus.grpc.test.utils.VertxGRPCTestProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(VertxGRPCTestProfile.class)
class VertxHelloWorldEndpointIT extends HelloWorldEndpointTestBase {

}
