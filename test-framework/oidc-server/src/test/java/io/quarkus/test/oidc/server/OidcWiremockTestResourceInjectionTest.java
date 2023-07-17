package io.quarkus.test.oidc.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.TestResourceManager;

/**
 * Validates the injection of {@link WireMockServer} objects into test instances by {@link OidcWiremockTestResource}.
 */
class OidcWiremockTestResourceInjectionTest {

    @Test
    void testWireMockServerInjection() {
        TestResourceManager manager = new TestResourceManager(CustomTest.class);
        manager.start();

        CustomTest test = new CustomTest();
        manager.inject(test);
        assertNotNull(test.server);
    }

    @QuarkusTestResource(OidcWiremockTestResource.class)
    public static class CustomTest {
        @OidcWireMock
        WireMockServer server;
    }
}
