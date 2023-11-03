package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.KOTLIN;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.RESTEASY_REACTIVE_CODESTART_RESOURCE_CLASS_NAME;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.RESTEASY_REACTIVE_CODESTART_RESOURCE_PATH;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

class CustomRESTEasyReactiveCodestartBuildIT {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("resteasy-reactive")
            .languages(JAVA, KOTLIN)
            .putData(RESTEASY_REACTIVE_CODESTART_RESOURCE_CLASS_NAME, "RESTEasyEndpoint")
            .putData(RESTEASY_REACTIVE_CODESTART_RESOURCE_PATH, "/resteasy")
            .build();

    @Test
    void testBuild() throws IOException {
        codestartTest.buildAllProjects();
    }

}
