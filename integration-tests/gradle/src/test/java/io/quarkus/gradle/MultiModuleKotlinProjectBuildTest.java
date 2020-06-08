package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

public class MultiModuleKotlinProjectBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("multi-module-kotlin-project");

        runGradleWrapper(projectDir, "clean", "build");
    }
}
