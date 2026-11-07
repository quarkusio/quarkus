package io.quarkus.gradle.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.testing.BaseGradleTest;

public class IncludedBuildDependencyTest extends BaseGradleTest {

    @Test
    void applicationDependsOnTheRootProjectOfAnIncludedBuild() throws Exception {
        // The root project of this build has no Java plugin, and has the same project path, ":", as the root project
        // of the included build the application depends on
        writeFile("settings.gradle.kts", """
                rootProject.name = "root"
                include("app")
                includeBuild("common")
                """);
        writeFile("app/build.gradle.kts", """
                plugins {
                    java
                    id("io.quarkus")
                }

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation(enforcedPlatform("io.quarkus:quarkus-bom:${project.property("version")}"))
                    implementation("io.quarkus:quarkus-arc")
                    implementation("org.acme:common:1.0")
                }
                """);
        writeFile("app/src/main/java/org/acme/App.java", """
                package org.acme;

                import jakarta.enterprise.context.ApplicationScoped;

                import org.acme.common.Greeting;

                @ApplicationScoped
                public class App {

                    public String greet() {
                        return Greeting.hello();
                    }
                }
                """);
        writeFile("common/settings.gradle.kts", """
                rootProject.name = "common"
                """);
        writeFile("common/build.gradle.kts", """
                plugins {
                    `java-library`
                }

                group = "org.acme"
                version = "1.0"
                """);
        writeFile("common/src/main/java/org/acme/common/Greeting.java", """
                package org.acme.common;

                public class Greeting {

                    public static String hello() {
                        return "hello";
                    }
                }
                """);
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        BuildResult result = buildResult(":app:quarkusBuild");

        // From the cache when an earlier test run built the same application
        assertThat(taskOutcomes(result).get(":app:quarkusBuild"))
                .describedAs("%s", result.getOutput())
                .isIn(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE);
        assertThat(jarsContaining(testProjectDir.resolve("app/build/quarkus-app/lib/main"), "org/acme/common/Greeting.class"))
                .describedAs("jars of the built application containing the included build's class")
                .hasSize(1);
    }

    private static List<Path> jarsContaining(Path dir, String entryName) throws IOException {
        List<Path> jars = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path jar : files.filter(file -> file.getFileName().toString().endsWith(".jar")).toList()) {
                try (JarFile jarFile = new JarFile(jar.toFile())) {
                    if (jarFile.getJarEntry(entryName) != null) {
                        jars.add(jar);
                    }
                }
            }
        }
        return jars;
    }
}
