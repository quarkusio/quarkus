package io.quarkus.grpc.examples.hello;

import io.quarkus.grpc.test.utils.O2NGRPCTestProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(O2NGRPCTestProfile.class)
class HelloWorldO2NServiceIT extends HelloWorldNewServiceTestBase {
    @Override
    protected int port() {
        return 8081;
    }

    @Override
    protected boolean skipEventloopTest() {
        return true; // cannot know for sure if we have enough verticles
    }
}
