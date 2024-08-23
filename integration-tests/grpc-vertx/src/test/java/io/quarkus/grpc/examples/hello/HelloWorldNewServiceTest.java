package io.quarkus.grpc.examples.hello;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HelloWorldNewServiceTest extends HelloWorldNewServiceTestBase {
    @Override
    protected int port() {
        return 9001;
    }
}
