package io.quarkus.smallrye.openapi.test.jaxrs;

import java.util.Optional;

import jakarta.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class OpenApiMPFilterTest {
    private static final String OPEN_API_PATH = "/q/openapi";

    public static class MyOASFilter implements OASFilter {

        final Config config = ConfigProvider.getConfig();

        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            Optional<String> maybeVersion = config.getOptionalValue("my.openapi.version", String.class);
            String version = maybeVersion.orElse("3.0.3");
            openAPI.setOpenapi(version);

            // Below is to test runtime filters that use CDI
            CDI.current().getBeanManager().createInstance();
        }

    }

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(OpenApiMPFilterTest.class)
                    .addAsResource(new StringAsset("""
                            mp.openapi.filter=io.quarkus.smallrye.openapi.test.jaxrs.OpenApiMPFilterTest$MyOASFilter
                            my.openapi.version=3.1.0"""),
                            "application.properties"));

    @Test
    public void testOpenApiFilterResource() {
        RestAssured.given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("openapi", Matchers.equalTo("3.1.0"));
    }
}
