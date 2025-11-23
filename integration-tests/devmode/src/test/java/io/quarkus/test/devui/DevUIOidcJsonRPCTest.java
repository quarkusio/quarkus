package io.quarkus.test.devui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIOidcJsonRPCTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest().withEmptyApplication();

    public DevUIOidcJsonRPCTest() {
        super("quarkus-oidc");
    }

    @Test
    public void testGetProperties() throws Exception {
        // Tests are disabled in the extension. Extension owners to add this
        // JsonNode properties = super.executeJsonRPCMethod("getProperties");
        // Assertions.assertNotNull(properties);
        // log.debug(properties.toPrettyString());
        // TODO: Add some more checks ?

    }

}
