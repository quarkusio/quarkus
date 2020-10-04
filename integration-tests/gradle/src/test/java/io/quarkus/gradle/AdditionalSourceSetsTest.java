package io.quarkus.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

/**
 * Test for verifying that the plugin can work with test-sources which are included as part of a custom source set.
 * E.g., functional-tests / integration-tests.
 */
public class AdditionalSourceSetsTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void executeFunctionalTest() throws URISyntaxException, IOException, InterruptedException {
        final File projectDir = getProjectDir("additional-source-sets");
        BuildResult result = runGradleWrapper(projectDir, "functionalTest");
        assertEquals(BuildResult.SUCCESS_OUTCOME, result.getTasks().get(":functionalTest"),
                "Failed to run tests defined in an alternate test sources directory");
    }
}
