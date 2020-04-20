package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class BasicMultiModuleProjectTestSetupTest extends QuarkusGradleTestBase {

    @Test
    public void testBasicMultiModuleTest() throws Exception {

        final File projectDir = getProjectDir("basic-multi-module-project-test-setup");
        
        BuildResult build = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("clean", "test"))
                .withProjectDir(projectDir)
                .build();
    }
}
