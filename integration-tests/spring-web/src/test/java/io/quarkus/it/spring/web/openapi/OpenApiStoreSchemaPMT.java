package io.quarkus.it.spring.web.openapi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;
import io.smallrye.openapi.runtime.io.Format;

public class OpenApiStoreSchemaPMT {

    private static final String directory = "target/generated/spring/";
    private static final String OPEN_API_DOT = "openapi.";

    @RegisterExtension
    static QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiController.class)
                    .addAsResource("test-roles.properties")
                    .addAsResource("test-users.properties"))
            .overrideConfigKey("quarkus.smallrye-openapi.store-schema-directory", directory)
            .setRun(true);

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void testOpenApiPathAccessResource() {
        Path outputDir = prodModeTestResults.getBuildDir().getParent();
        Path json = outputDir.resolve(Paths.get(directory, OPEN_API_DOT + Format.JSON.toString().toLowerCase()));
        Assertions.assertTrue(Files.exists(json));
        Path yaml = outputDir.resolve(Paths.get(directory, OPEN_API_DOT + Format.YAML.toString().toLowerCase()));
        Assertions.assertTrue(Files.exists(yaml));
    }
}
