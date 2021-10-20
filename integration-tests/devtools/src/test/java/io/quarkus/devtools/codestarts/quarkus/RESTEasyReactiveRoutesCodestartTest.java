package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class RESTEasyReactiveRoutesCodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("reactive-routes-codestart")
            .languages(JAVA, KOTLIN)
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.MyDeclarativeRoutes");
        codestartTest.checkGeneratedTestSource("org.acme.MyDeclarativeRoutesTest");
    }

    @Test
    void buildAllProjects() throws Throwable {
        codestartTest.buildAllProjects();
    }
}
