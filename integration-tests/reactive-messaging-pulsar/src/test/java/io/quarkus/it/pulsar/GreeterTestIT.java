package io.quarkus.it.pulsar;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class GreeterTestIT extends GreeterTest {

    public int getPort() {
        // In native mode, the port is not injected, so we need to retrieve it from the config
        // I don't know how to set this up without hardcoding
        return 9000;
    }
}
