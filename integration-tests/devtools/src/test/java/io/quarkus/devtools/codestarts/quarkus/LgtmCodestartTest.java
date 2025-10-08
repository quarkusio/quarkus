package io.quarkus.devtools.codestarts.quarkus;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;

public class LgtmCodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("lgtm")
            .languages(JAVA)
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.SimpleEndpoint");
        codestartTest.checkGeneratedTestSource("org.acme.SimpleTest");
    }

    @Test
    @EnabledIfSystemProperty(named = "build-projects", matches = "true")
    void buildAllProjectsForLocalUse() throws Throwable {
        codestartTest.buildAllProjects();
    }
}
