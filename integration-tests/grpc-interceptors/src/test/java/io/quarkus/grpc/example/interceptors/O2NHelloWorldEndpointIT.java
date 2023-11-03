package io.quarkus.grpc.example.interceptors;

import io.quarkus.grpc.test.utils.O2NGRPCTestProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(O2NGRPCTestProfile.class)
class O2NHelloWorldEndpointIT extends HelloWorldEndpointTestBase {

}
