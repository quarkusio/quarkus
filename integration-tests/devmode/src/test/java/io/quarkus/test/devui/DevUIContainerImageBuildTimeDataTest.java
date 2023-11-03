package io.quarkus.test.devui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIBuildTimeDataTest;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIContainerImageBuildTimeDataTest extends DevUIBuildTimeDataTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    public DevUIContainerImageBuildTimeDataTest() {
        super("io.quarkus.quarkus-container-image");
    }

    @Test
    public void testBuilderTypes() throws Exception {
        JsonNode builderTypes = super.getBuildTimeData("builderTypes");
        Assertions.assertNotNull(builderTypes);
        Assertions.assertTrue(builderTypes.isArray());
    }

}
