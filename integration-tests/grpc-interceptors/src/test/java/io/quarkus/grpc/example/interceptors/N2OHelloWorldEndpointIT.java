package io.quarkus.grpc.example.interceptors;

import io.quarkus.grpc.test.utils.N2OGRPCTestProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(N2OGRPCTestProfile.class)
class N2OHelloWorldEndpointIT extends HelloWorldEndpointTestBase {

}
