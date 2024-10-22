package io.quarkus.test.devui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.BuildTimeDataResolver;
import io.quarkus.devui.tests.DevUITest;
import io.quarkus.devui.tests.Namespace;
import io.quarkus.test.QuarkusDevModeTest;

@DevUITest(@Namespace("io.quarkus.quarkus-container-image"))
public class DevUIContainerImageBuildTimeDataTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    @Test
    public void testBuilderTypes(BuildTimeDataResolver buildTimeDataResolver) throws Exception {
        JsonNode builderTypes = buildTimeDataResolver
                .request()
                .send()
                .get("builderTypes");
        Assertions.assertNotNull(builderTypes);
        Assertions.assertTrue(builderTypes.isArray());
    }

}
