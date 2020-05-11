package io.quarkus.gradle;

import java.io.File;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;


public class MultiModuleKotlinProjectBuildTest extends QuarkusGradleTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("multi-module-kotlin-project");

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("clean", "build"))
                .withProjectDir(projectDir)
                .build();
    }
}
