package io.quarkus.spring.cloud.config.client.runtime;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class GreetingResourceIT extends GreetingResourceTest {

    @Override
    protected String getExpectedValue() {
        return "hello from spring cloud config server";
    }
}
