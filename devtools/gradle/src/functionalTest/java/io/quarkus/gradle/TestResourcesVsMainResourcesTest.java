package io.quarkus.gradle;

import java.io.File;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;


public class TestResourcesVsMainResourcesTest extends QuarkusGradleTestBase {

    @Test
    public void test() throws Exception {

        final File projectDir = getProjectDir("test-resources-vs-main-resources");

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("test"))
                .withProjectDir(projectDir)
                .build();
    }
}
