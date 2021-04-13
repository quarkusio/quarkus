package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;
import static io.quarkus.devtools.testing.SnapshotTesting.checkContains;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class PicocliCodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("picocli")
            .languages(JAVA, KOTLIN)
            .skipGenerateRealDataProject()
            .build();

    @RegisterExtension
    public static QuarkusCodestartTest codestartGradleTest = QuarkusCodestartTest.builder()
            .codestarts("picocli")
            .buildTool(BuildTool.GRADLE)
            .languages(JAVA)
            .skipGenerateRealDataProject()
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.picocli.EntryCommand");
        codestartTest.checkGeneratedSource("org.acme.picocli.GoodbyeCommand");
        codestartTest.checkGeneratedSource("org.acme.picocli.HelloCommand");
        codestartTest.checkGeneratedSource("org.acme.picocli.GreetingService");

        codestartTest.assertThatGeneratedFile(JAVA, "README.md")
                .satisfies(checkContains("./mvnw compile quarkus:dev -Dquarkus.args='hello --first-name=Quarky"));

        codestartGradleTest.assertThatGeneratedFile(JAVA, "README.md")
                .satisfies(checkContains("./gradlew quarkusDev --quarkus-args='hello --first-name=Quarky'"));
    }
}
