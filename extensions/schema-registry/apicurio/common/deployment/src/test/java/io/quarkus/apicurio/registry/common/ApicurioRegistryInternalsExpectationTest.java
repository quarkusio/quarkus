package io.quarkus.apicurio.registry.common;

import org.junit.jupiter.api.Test;

import io.apicurio.rest.client.spi.ApicurioHttpClientFactory;

public class ApicurioRegistryInternalsExpectationTest {
    @Test
    public void test() throws NoSuchFieldException {
        // we need this to reset the client in continuous testing
        ApicurioHttpClientFactory.class.getDeclaredField("providerReference");
    }
}
