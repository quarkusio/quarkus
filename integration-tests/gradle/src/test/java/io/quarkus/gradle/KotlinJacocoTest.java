package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class KotlinJacocoTest extends QuarkusGradleWrapperTestBase {

    /**
     * This test should probably do more than simply verify a successful command execution.
     * It was originally added to make sure the process doesn't fail (which it used to).
     */
    @Test
    public void testFastJarFormatWorks() throws Exception {
        final File projectDir = getProjectDir("kotlin-jacoco");
        runGradleWrapper(projectDir, "clean", "test");
    }
}
