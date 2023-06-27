package io.quarkus.devui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIBuildTimeDataTest;
import io.quarkus.test.QuarkusDevModeTest;

public class ExtensionsTest extends DevUIBuildTimeDataTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest().withEmptyApplication();

    public ExtensionsTest() {
        super("devui");
    }

    @Test
    public void testGetExtensions() throws Exception {
        JsonNode extensionsResponse = super.getBuildTimeData("extensions");
        Assertions.assertNotNull(extensionsResponse);

        JsonNode activeExtensions = extensionsResponse.get("active");
        Assertions.assertNotNull(activeExtensions);
        Assertions.assertTrue(activeExtensions.isArray());

        JsonNode inactiveExtensions = extensionsResponse.get("inactive");
        Assertions.assertNotNull(inactiveExtensions);
        Assertions.assertTrue(inactiveExtensions.isArray());

    }

}
