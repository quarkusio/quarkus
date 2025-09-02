package io.quarkus.devui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIBuildTimeDataTest;
import io.quarkus.test.QuarkusDevModeTest;

public class DevServicesTest extends DevUIBuildTimeDataTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest().withEmptyApplication();

    public DevServicesTest() {
        super("devui");
    }

    @Test
    public void testGetExtensions() throws Exception {
        JsonNode devServicesResponse = super.getBuildTimeData("devServices");
        Assertions.assertNotNull(devServicesResponse);
        Assertions.assertTrue(devServicesResponse.isArray());
    }

}
