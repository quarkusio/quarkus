package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.SPRING_WEB_CODESTART_RESOURCE_CLASS_NAME;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.SPRING_WEB_CODESTART_RESOURCE_PATH;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

class CustomSpringWebCodestartBuildIT {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .codestarts("spring-web")
            .putData(SPRING_WEB_CODESTART_RESOURCE_CLASS_NAME, "SpringWebEndpoint")
            .putData(SPRING_WEB_CODESTART_RESOURCE_PATH, "/springweb")
            .languages(JAVA)
            .build();

    @Test
    void testBuild() throws IOException {
        codestartTest.buildAllProjects();
    }

}
