package io.quarkus.smallrye.openapi.test.jaxrs;

import static io.restassured.RestAssured.given;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.test.QuarkusExtensionTest;

public class OpenApiMPInheritanceOpenApiFilterAnnotationTest {
    private static final String OPEN_API_PATH = "/q/openapi";
    private static final String OPEN_API_CUSTOM_PATH = "/q/openapi-custom";

    public static class MyOASFilter implements OASFilter {

        int patchVersion = 0;

        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            openAPI.setOpenapi("3.0." + patchVersion);
            patchVersion++;
        }
    }

    @Singleton
    @OpenApiFilter(documentNames = "custom", stages = OpenApiFilter.RunStage.RUNTIME_PER_REQUEST)
    public static class MyCustomOASFilter extends MyOASFilter implements OASFilter {
    }

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(OpenApiMPInheritanceOpenApiFilterAnnotationTest.class)
                    .addAsResource(
                            new StringAsset(
                                    """
                                            mp.openapi.filter=io.quarkus.smallrye.openapi.test.jaxrs.OpenApiMPInheritanceOpenApiFilterAnnotationTest$MyOASFilter
                                            quarkus.smallrye-openapi.custom.info-title=Custom documentation
                                            quarkus.smallrye-openapi.open-api-version=3.1.0"""),
                            "application.properties"));

    @Test
    public void testOpenApiFilterResource() {
        // validating that MyOASFilter does not change version to 3.0.0, but keeps configured verison
        given().header("Accept", "application/json")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("openapi", Matchers.equalTo("3.1.0"));

        // validating that MyOASFilter on first request returns 0 - so not run during RUNTIME_STARTUP
        given().header("Accept", "application/json")
                .when().get(OPEN_API_CUSTOM_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("openapi", Matchers.equalTo("3.0.0"));

        // validating that MyOASFilter executes during request by increasing version numbers
        given().header("Accept", "application/json")
                .when().get(OPEN_API_CUSTOM_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("openapi", Matchers.equalTo("3.0.1"));

        given().header("Accept", "application/json")
                .when().get(OPEN_API_CUSTOM_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("openapi", Matchers.equalTo("3.0.2"));
    }
}
