package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class JacocoMultiModuleTest extends QuarkusGradleWrapperTestBase {

    /**
     * This test should probably do more than simply verify a successful command execution.
     * It was originally added to make sure the process doesn't fail.
     */
    @Test
    public void testJacoco() throws Exception {
        final File projectDir = getProjectDir("basic-multi-module-project-jacoco");
        runGradleWrapper(projectDir, "clean", "test");
    }
}
