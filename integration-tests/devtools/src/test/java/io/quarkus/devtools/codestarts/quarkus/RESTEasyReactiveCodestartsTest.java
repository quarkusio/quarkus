package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class RESTEasyReactiveCodestartsTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("resteasy-reactive", "resteasy-reactive-qute")
            .languages(JAVA, KOTLIN)
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.SomePage");
        codestartTest.checkGeneratedSource("org.acme.ReactiveGreetingResource");
        codestartTest.assertThatGeneratedFileMatchSnapshot(JAVA, "src/main/resources/templates/page.qute.html");
    }

    @Test
    void buildAllProjects() throws Throwable {
        codestartTest.buildAllProjects();
    }
}
