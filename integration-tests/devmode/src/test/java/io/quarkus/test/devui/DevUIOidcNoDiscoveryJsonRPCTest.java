package io.quarkus.test.devui;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIOidcNoDiscoveryJsonRPCTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(createApplicationProperties(),
                    "application.properties"));

    public DevUIOidcNoDiscoveryJsonRPCTest() {
        super("quarkus-oidc");
    }

    @Test
    public void testGetProperties() throws Exception {
        JsonNode properties = super.executeJsonRPCMethod("getProperties");
        Assertions.assertNotNull(properties);
        log.debug(properties.toPrettyString());
        // TODO: Add some more checks ?

    }

    private static StringAsset createApplicationProperties() {
        return new StringAsset("quarkus.oidc.auth-server-url=http://localhost/oidc\n"
                + "quarkus.oidc.client-id=client\n"
                + "quarkus.oidc.discovery-enabled=false\n"
                + "quarkus.oidc.introspection-path=introspect\n");

    }
}
