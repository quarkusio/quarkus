package io.quarkus.devtools.codestarts.testing;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.SCALA;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

class QuarkusCodestartTestExtensionTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("resteasy")
            .languages(JAVA, KOTLIN, SCALA)
            .skipGenerateRealDataProject()
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.GreetingResource");
        codestartTest.checkGeneratedTestSource("org.acme.GreetingResourceTest");
        codestartTest.checkGeneratedTestSource("org.acme.NativeGreetingResourceIT");
    }

}
