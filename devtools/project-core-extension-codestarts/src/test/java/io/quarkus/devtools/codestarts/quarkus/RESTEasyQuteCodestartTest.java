package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class RESTEasyQuteCodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("resteasy-qute")
            .languages(JAVA, KOTLIN)
            .skipGenerateRealDataProject()
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.resteasyqute.Quark");
        codestartTest.checkGeneratedSource("org.acme.resteasyqute.QuteResource");
        codestartTest.assertThatGeneratedFileMatchSnapshot(JAVA, "src/main/resources/templates/page.qute.html");
        codestartTest.assertThatGeneratedFileMatchSnapshot(KOTLIN, "src/main/resources/templates/page.qute.html");
    }
}
