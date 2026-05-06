package io.quarkus.grpc.examples.hello;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class HelloWorldNewServiceIT extends HelloWorldNewServiceTestBase {
    @Override
    protected int port() {
        return 8081;
    }
}
