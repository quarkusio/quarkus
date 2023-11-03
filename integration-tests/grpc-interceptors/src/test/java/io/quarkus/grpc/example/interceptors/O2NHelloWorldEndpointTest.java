package io.quarkus.grpc.example.interceptors;

import io.quarkus.grpc.test.utils.O2NGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(O2NGRPCTestProfile.class)
class O2NHelloWorldEndpointTest extends HelloWorldEndpointTestBase {

}
