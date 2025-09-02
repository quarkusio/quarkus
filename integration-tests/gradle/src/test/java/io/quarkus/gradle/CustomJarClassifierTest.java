package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class CustomJarClassifierTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {
        final File projectDir = getProjectDir("custom-jar-classifier");
        runGradleWrapper(projectDir, "clean", "build");
    }
}
