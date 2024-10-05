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
public class DevServicesTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest().withEmptyApplication();

    @Test
    public void testGetExtensions(BuildTimeDataResolver buildTimeDataResolver) throws Exception {
        JsonNode devServicesResponse = buildTimeDataResolver
                .request()
                .send()
                .get("devServices");
        Assertions.assertNotNull(devServicesResponse);
        Assertions.assertTrue(devServicesResponse.isArray());
    }

}
