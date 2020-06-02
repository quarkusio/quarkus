package io.quarkus.gradle;

import java.io.File;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;


public class CustomFileSystemProviderTest extends QuarkusGradleTestBase {

    @Test
    public void test() throws Exception {

        final File projectDir = getProjectDir("custom-filesystem-provider");

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("clean", ":application:test"))
                .withProjectDir(projectDir)
                .build();
    }
}
