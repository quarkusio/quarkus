package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class BasicMultiModuleProjectTestSetupTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleTest() throws Exception {

        final File projectDir = getProjectDir("basic-multi-module-project-test-setup");

        runGradleWrapper(projectDir, "clean", "test");
    }
}
