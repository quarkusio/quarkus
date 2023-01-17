package io.quarkus.grpc.example.interceptors;

import io.quarkus.grpc.test.utils.N2OGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(N2OGRPCTestProfile.class)
class N2OHelloWorldEndpointTest extends HelloWorldEndpointTestBase {

}
