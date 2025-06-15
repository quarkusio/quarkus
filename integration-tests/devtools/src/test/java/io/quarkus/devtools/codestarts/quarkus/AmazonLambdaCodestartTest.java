package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class AmazonLambdaCodestartTest {
    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder().codestarts("amazon-lambda")
            .languages(JAVA).build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.lambda.GreetingLambda");
        codestartTest.checkGeneratedSource("org.acme.lambda.Person");

        codestartTest.checkGeneratedTestSource("org.acme.lambda.LambdaHandlerTest");
        codestartTest.checkGeneratedTestSource("org.acme.lambda.LambdaHandlerTestIT");
    }

    @Test
    @EnabledIfSystemProperty(named = "build-projects", matches = "true")
    void buildAllProjectsForLocalUse() throws Throwable {
        codestartTest.buildAllProjects();
    }
}
