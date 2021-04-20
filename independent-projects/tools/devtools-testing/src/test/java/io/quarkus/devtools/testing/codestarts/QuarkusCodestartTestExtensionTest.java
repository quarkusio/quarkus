package io.quarkus.devtools.testing.codestarts;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.SCALA;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
