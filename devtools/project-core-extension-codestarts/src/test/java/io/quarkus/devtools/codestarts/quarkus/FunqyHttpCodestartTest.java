package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class FunqyHttpCodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("funqy-http")
            .languages(JAVA)
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.MyFunctions");
        codestartTest.checkGeneratedTestSource("org.acme.MyFunctionsTest");
        codestartTest.checkGeneratedTestSource("org.acme.MyFunctionsIT");
    }

    @Test
    @EnabledIfSystemProperty(named = "build-projects", matches = "true")
    void buildAllProjectsForLocalUse() throws Throwable {
        codestartTest.buildAllProjects();
    }

}
