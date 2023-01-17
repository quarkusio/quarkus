package io.quarkus.grpc.examples.hello;

import io.quarkus.grpc.test.utils.O2NGRPCTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(O2NGRPCTestProfile.class)
class HelloWorldO2NServiceTest extends HelloWorldNewServiceTestBase {
    @Override
    protected int port() {
        return 8081;
    }
}
