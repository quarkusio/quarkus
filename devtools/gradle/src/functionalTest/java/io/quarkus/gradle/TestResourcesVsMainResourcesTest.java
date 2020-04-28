package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class TestResourcesVsMainResourcesTest extends QuarkusGradleTestBase {

    @Test
    public void test() throws Exception {

        final File projectDir = getProjectDir("test-resources-vs-main-resources");
        
        BuildResult build = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("test"))
                .withProjectDir(projectDir)
                .build();
    }
}
