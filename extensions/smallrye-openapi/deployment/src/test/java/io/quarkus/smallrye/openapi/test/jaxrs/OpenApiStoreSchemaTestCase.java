package io.quarkus.smallrye.openapi.test.jaxrs;

import java.io.IOException;
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

    private static String directory = "target/generated/jax-rs/";
    private static final String OPEN_API_DOT = "openapi.";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiResource.class, ResourceBean.class, NamingOASFilter.class)
                    .addAsResource(new StringAsset("mp.openapi.filter=io.quarkus.smallrye.openapi.test.jaxrs.NamingOASFilter\n"
                            + "quarkus.smallrye-openapi.store-schema-directory=" + directory),
                            "application.properties"));

    @Test
    public void testOpenApiPathAccessResource() throws IOException {
        Path json = Paths.get(directory, OPEN_API_DOT + Format.JSON.toString().toLowerCase());
        Assertions.assertTrue(Files.exists(json));
        Path yaml = Paths.get(directory, OPEN_API_DOT + Format.YAML.toString().toLowerCase());
        Assertions.assertTrue(Files.exists(yaml));

        // Also check if the custom filter applied
        String content = new String(Files.readAllBytes(json));
        Assertions.assertTrue(content.contains(" \"title\" : \"Here my title from a filter\""));
    }
}
