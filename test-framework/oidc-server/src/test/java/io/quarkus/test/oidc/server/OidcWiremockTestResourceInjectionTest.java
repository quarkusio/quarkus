package io.quarkus.test.oidc.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.WithTestResource;

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

    @WithTestResource(value = OidcWiremockTestResource.class, restrictToAnnotatedClass = false)
    public static class CustomTest {
        @OidcWireMock
        WireMockServer server;
    }
}
