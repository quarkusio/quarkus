package io.quarkus.smallrye.openapi.test.vertx;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.openapi.runtime.io.Format;

public class OpenApiStoreSchemaTestCase {

    private static String directory = "target/generated/vertx/";
    private static final String OPEN_API_DOT = "openapi.";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiRoute.class)
                    .addAsResource(new StringAsset("quarkus.smallrye-openapi.store-schema-directory=" + directory),
                            "application.properties"));

    @Test
    public void testOpenApiPathAccessResource() {
        Path json = Paths.get(directory, OPEN_API_DOT + Format.JSON.toString().toLowerCase());
        Assertions.assertTrue(Files.exists(json));
        Path yaml = Paths.get(directory, OPEN_API_DOT + Format.YAML.toString().toLowerCase());
        Assertions.assertTrue(Files.exists(yaml));
    }
}
