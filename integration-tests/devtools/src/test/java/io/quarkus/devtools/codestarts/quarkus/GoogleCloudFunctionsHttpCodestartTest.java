package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class GoogleCloudFunctionsHttpCodestartTest {
    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("google-cloud-functions-http")
            .languages(JAVA)
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.googlecloudfunctions.GreetingFunqy");
        codestartTest.checkGeneratedSource("org.acme.googlecloudfunctions.GreetingResource");
        codestartTest.checkGeneratedSource("org.acme.googlecloudfunctions.GreetingRoutes");
        codestartTest.checkGeneratedSource("org.acme.googlecloudfunctions.GreetingServlet");
    }

    @Test
    @EnabledIfSystemProperty(named = "build-projects", matches = "true")
    void buildAllProjectsForLocalUse() throws Throwable {
        codestartTest.buildAllProjects();
    }
}
