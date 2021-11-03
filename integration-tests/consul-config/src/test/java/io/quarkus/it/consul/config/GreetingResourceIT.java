package io.quarkus.it.consul.config;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class GreetingResourceIT extends GreetingResourceTest {

    @Override
    protected String getExpectedValue() {
        return "Hello from Consul";
    }
}
