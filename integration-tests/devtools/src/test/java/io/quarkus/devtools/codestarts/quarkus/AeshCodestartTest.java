package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;
import static io.quarkus.devtools.testing.SnapshotTesting.checkContains;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class AeshCodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("aesh")
            .languages(JAVA, KOTLIN)
            .build();

    @RegisterExtension
    public static QuarkusCodestartTest codestartGradleTest = QuarkusCodestartTest.builder()
            .codestarts("aesh")
            .buildTool(BuildTool.GRADLE)
            .languages(JAVA)
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.HelloCommand");
        codestartTest.checkGeneratedTestSource("org.acme.HelloCommandTest");

        codestartTest.assertThatGeneratedFile(JAVA, "README.md")
                .satisfies(checkContains("./mvnw quarkus:dev -Dquarkus.args='--name=Quarky"));

        codestartGradleTest.assertThatGeneratedFile(JAVA, "README.md")
                .satisfies(checkContains("./gradlew quarkusDev --quarkus-args='--name=Quarky'"));
    }

    @Test
    @EnabledIfSystemProperty(named = "build-projects", matches = "true")
    void buildAllProjectsForLocalUse() throws Throwable {
        codestartTest.buildAllProjects();
    }
}
