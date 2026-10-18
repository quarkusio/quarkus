package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;
import static io.quarkus.devtools.testing.SnapshotTesting.checkContains;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;
import io.quarkus.maven.dependency.ArtifactKey;

public class KotlinSerializationCodestartTest {
    @RegisterExtension
    public static QuarkusCodestartTest codestartMavenTest = QuarkusCodestartTest.builder()
            .extension(ArtifactKey.fromString("io.quarkus:quarkus-rest-kotlin-serialization"))
            .languages(KOTLIN)
            .buildTool(BuildTool.MAVEN)
            .build();

    @RegisterExtension
    public static QuarkusCodestartTest codestartGradleTest = QuarkusCodestartTest.builder()
            .extension(ArtifactKey.fromString("io.quarkus:quarkus-rest-kotlin-serialization"))
            .languages(KOTLIN)
            .buildTool(BuildTool.GRADLE)
            .build();

    @RegisterExtension
    public static QuarkusCodestartTest codestartGradleKotlinTest = QuarkusCodestartTest.builder()
            .extension(ArtifactKey.fromString("io.quarkus:quarkus-rest-kotlin-serialization"))
            .languages(KOTLIN)
            .buildTool(BuildTool.GRADLE_KOTLIN_DSL)
            .build();

    @Test
    void testMavenContent() throws Throwable {
        codestartMavenTest.assertThatGeneratedFileMatchSnapshot(KOTLIN, "pom.xml")
                .satisfies(checkContains("<plugin>kotlinx-serialization</plugin>"))
                .satisfies(checkContains("<artifactId>kotlin-maven-serialization</artifactId>"));
    }

    @Test
    void testGradleContent() throws Throwable {
        codestartGradleTest.assertThatGeneratedFileMatchSnapshot(KOTLIN, "build.gradle")
                .satisfies(checkContains("id 'org.jetbrains.kotlin.plugin.serialization' version "));
    }

    @Test
    void testGradleKotlinContent() throws Throwable {
        codestartGradleKotlinTest.assertThatGeneratedFileMatchSnapshot(KOTLIN, "build.gradle.kts")
                .satisfies(checkContains("kotlin(\"plugin.serialization\") version "));
    }

    @Test
    void buildAllProjectsMaven() throws Throwable {
        codestartMavenTest.buildAllProjects();
    }

    @Test
    void buildAllProjectsGradle() throws Throwable {
        codestartGradleTest.buildAllProjects();
    }
}
