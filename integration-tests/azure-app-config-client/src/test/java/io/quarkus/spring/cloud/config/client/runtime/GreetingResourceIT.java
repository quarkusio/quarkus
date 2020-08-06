package io.quarkus.spring.cloud.config.client.runtime;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class GreetingResourceIT extends GreetingResourceTest {

    @Override
    protected String getExpectedValue() {
        return "hello from azure app config server";
    }
}
