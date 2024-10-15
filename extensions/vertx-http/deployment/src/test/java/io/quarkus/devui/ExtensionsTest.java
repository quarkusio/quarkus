package io.quarkus.devui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.BuildTimeDataResolver;
import io.quarkus.devui.tests.DevUITest;
import io.quarkus.devui.tests.Namespace;
import io.quarkus.test.QuarkusDevModeTest;

@DevUITest(@Namespace("devui"))
public class ExtensionsTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest().withEmptyApplication();

    @Test
    public void testGetExtensions(BuildTimeDataResolver buildTimeDataResolver) throws Exception {
        JsonNode extensionsResponse = buildTimeDataResolver
                .request()
                .send()
                .get("extensions");
        Assertions.assertNotNull(extensionsResponse);

        JsonNode activeExtensions = extensionsResponse.get("active");
        Assertions.assertNotNull(activeExtensions);
        Assertions.assertTrue(activeExtensions.isArray());

        JsonNode inactiveExtensions = extensionsResponse.get("inactive");
        Assertions.assertNotNull(inactiveExtensions);
        Assertions.assertTrue(inactiveExtensions.isArray());

    }

}
