package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.SCALA;
import static io.quarkus.devtools.testing.FakeExtensionCatalog.FAKE_EXTENSION_CATALOG;
import static io.quarkus.devtools.testing.FakeExtensionCatalog.FAKE_QUARKUS_CODESTART_CATALOG;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class RESTEasyReactiveCodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .quarkusCodestartCatalog(FAKE_QUARKUS_CODESTART_CATALOG)
            .extensionCatalog(FAKE_EXTENSION_CATALOG)
            .codestarts("resteasy-reactive")
            .languages(JAVA, KOTLIN, SCALA)
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.ReactiveGreetingResource");
        codestartTest.checkGeneratedTestSource("org.acme.ReactiveGreetingResourceTest");
        codestartTest.checkGeneratedTestSource("org.acme.NativeReactiveGreetingResourceIT");
    }

}
