package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class WebSocketsCodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("websockets")
            .languages(JAVA, KOTLIN)
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.StartWebSocket");
    }

    @Test
    @EnabledIfSystemProperty(named = "build-projects", matches = "true")
    void buildAllProjectsForLocalUse() throws Throwable {
        codestartTest.buildAllProjects();
    }
}
