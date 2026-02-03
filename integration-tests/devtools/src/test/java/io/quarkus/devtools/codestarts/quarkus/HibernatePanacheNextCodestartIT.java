package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;
import io.quarkus.maven.ArtifactKey;

public class HibernatePanacheNextCodestartIT {

    @RegisterExtension
    public static QuarkusCodestartTest codestartMavenTest = QuarkusCodestartTest.builder()
            .codestarts("hibernate-orm")
            .extension(new ArtifactKey("io.quarkus", "quarkus-jdbc-h2"))
            .extension(new ArtifactKey("io.quarkus", "quarkus-hibernate-panache-next"))
            .languages(JAVA)
            .build();

    @RegisterExtension
    public static QuarkusCodestartTest codestartGradleTest = QuarkusCodestartTest.builder()
            .codestarts("hibernate-orm")
            .extension(new ArtifactKey("io.quarkus", "quarkus-jdbc-h2"))
            .extension(new ArtifactKey("io.quarkus", "quarkus-hibernate-panache-next"))
            .buildTool(BuildTool.GRADLE)
            .languages(JAVA)
            .build();

    @RegisterExtension
    public static QuarkusCodestartTest codestartGradleKotlinTest = QuarkusCodestartTest.builder()
            .codestarts("hibernate-orm")
            .extension(new ArtifactKey("io.quarkus", "quarkus-jdbc-h2"))
            .extension(new ArtifactKey("io.quarkus", "quarkus-hibernate-panache-next"))
            .buildTool(BuildTool.GRADLE_KOTLIN_DSL)
            .languages(JAVA)
            .build();

    @Test
    void testMavenContent() throws Throwable {
        codestartMavenTest.checkGeneratedSource("org.acme.MyEntity");
        codestartMavenTest.assertThatGeneratedFileMatchSnapshot(JAVA, "src/main/resources/import.sql");
    }

    @Test
    void testMavenBuild() throws Throwable {
        codestartMavenTest.buildAllProjects();
        Path metamodelPath = codestartMavenTest.getProjectWithRealDataDir(JAVA).resolve("target/generated-sources/annotations/"
                + codestartMavenTest.repackagedClassName("org.acme.MyEntity_").replace('.', '/') + ".java");
        Assertions.assertTrue(Files.exists(metamodelPath));
    }

    @Test
    void testGradleBuild() throws Throwable {
        codestartGradleTest.buildAllProjects();
        Path metamodelPath = codestartGradleTest.getProjectWithRealDataDir(JAVA)
                .resolve("build/generated/sources/annotationProcessor/java/main/"
                        + codestartGradleTest.repackagedClassName("org.acme.MyEntity_").replace('.', '/') + ".java");
        Assertions.assertTrue(Files.exists(metamodelPath));
    }

    @Test
    void testGradleKotlinBuild() throws Throwable {
        codestartGradleKotlinTest.buildAllProjects();
        Path metamodelPath = codestartGradleKotlinTest.getProjectWithRealDataDir(JAVA)
                .resolve("build/generated/sources/annotationProcessor/java/main/"
                        + codestartGradleKotlinTest.repackagedClassName("org.acme.MyEntity_").replace('.', '/') + ".java");
        Assertions.assertTrue(Files.exists(metamodelPath));
    }
}
