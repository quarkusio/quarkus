package io.quarkus.gradle.tasks;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class JavaToolchainTest {

    @TempDir
    Path testProjectDir;

    @Test
    void quarkusIsUsingJavaToolchain() throws Exception {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/toolchain/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("build", "--info", "--stacktrace", "--build-cache", "--configuration-cache")
                // .build() checks whether the build failed, which is good enough for this test
                .build();
    }

    @Test
    void quarkusPluginCanOverrideJavaToolchain() throws Exception {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/toolchain/custom");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("build", "--info", "--stacktrace", "--build-cache", "--configuration-cache")
                // .build() checks whether the build failed, which is good enough for this test
                .build();
    }

    @Test
    void quarkusPluginFailsWithIncompatibleToolchains() throws Exception {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/toolchain/fail");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        BuildResult buildResult = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("build", "--info", "--stacktrace", "--build-cache", "--configuration-cache")
                .buildAndFail();

        assertThat(buildResult.task(":quarkusAppPartsBuild").getOutcome()).isEqualTo(TaskOutcome.FAILED);
        assertThat(buildResult.getOutput()).contains("java.lang.UnsupportedClassVersionError");
    }
}
