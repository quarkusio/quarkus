package io.quarkus.smallrye.openapi.test.jaxrs;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test that If a filter is registered using mp.openapi.filter, its subclasses only get executed if they are registered using
 * OpenApiFilter annotation. The documentName and runstage of OpenApiFilter annotation should be taken for the subclass filter.
 */
public class OpenApiMPInheritanceOpenApiFilterAnnotationTest {
    private static final String OPEN_API_DEFAULT_PATH = "/q/openapi";
    private static final String OPEN_API_CUSTOM_PATH = "/q/openapi-custom";

    private static final String X_MP = "x-mp";
    private static final String X_OPENAPI_FILTER = "x-openapi-filter";
    private static final String X_SHOULD_NOT_RUN = "x-should-not-run";

    public static class MyMPFilter implements OASFilter {

        private static int version = 0;

        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            openAPI.addExtension(X_MP, version);
            version++;
        }
    }

    @OpenApiFilter(documentNames = "custom", stages = OpenApiFilter.RunStage.RUNTIME_PER_REQUEST)
    public static class MyRequestFilter extends MyMPFilter implements OASFilter {
        private static int version = 0;

        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            openAPI.addExtension(X_OPENAPI_FILTER, version);
            version++;
        }
    }

    public static class IShouldNotRun extends MyMPFilter implements OASFilter {
        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            openAPI.addExtension(X_SHOULD_NOT_RUN, true);
        }
    }

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClass(OpenApiMPInheritanceOpenApiFilterAnnotationTest.class)
                    .addAsResource(
                            new StringAsset(
                                    """
                                            mp.openapi.filter=io.quarkus.smallrye.openapi.test.jaxrs.OpenApiMPInheritanceOpenApiFilterAnnotationTest$MyMPFilter
                                            quarkus.smallrye-openapi.custom.info-title=Custom documentation"""),
                            "application.properties"));

    @Test
    public void testOpenApiFilterResource() {
        // remember: MyMPFilter (- ant its version field) is shared across documents. Which is why we assert notNullValue for it on the first 2 calls

        // validating that MyMPFilter set the extension
        Integer defaultDocX_MP = given().header("Accept", "application/json")
                .when().get(OPEN_API_DEFAULT_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(X_MP, notNullValue())
                .body(X_OPENAPI_FILTER, nullValue())
                .extract().body().path(X_MP);
        // Also validate for the custom document, that MyCountingStaticOASFilter is run
        Integer customDocX_MP = given().header("Accept", "application/json")
                .when().get(OPEN_API_CUSTOM_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(X_MP, notNullValue()).body(X_OPENAPI_FILTER, equalTo(0))
                .extract().body().path(X_MP);

        // Validate that MyMPFilter is not run another time. The filter is registered through mp.openapi.filter, which is implicitly using runstage RUNTIME_STARTUP.
        given().header("Accept", "application/json")
                .when().get(OPEN_API_DEFAULT_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(X_MP, equalTo(defaultDocX_MP))
                .body(X_OPENAPI_FILTER, nullValue());
        // Also validate for the custom document, that MyRequestFilter is run again
        given().header("Accept", "application/json")
                .when().get(OPEN_API_CUSTOM_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(X_MP, equalTo(customDocX_MP)).body(X_OPENAPI_FILTER, equalTo(1));

        given().header("Accept", "application/json")
                .when().get(OPEN_API_CUSTOM_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(X_OPENAPI_FILTER, equalTo(2));

        // Validate that MyRequestFilter is not executed for the default document
        given().header("Accept", "application/json")
                .when().get(OPEN_API_DEFAULT_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(X_OPENAPI_FILTER, nullValue());

        // Validate that IShouldNotRun is run for none of these documents
        given().header("Accept", "application/json")
                .when().get(OPEN_API_DEFAULT_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(X_SHOULD_NOT_RUN, nullValue());

        given().header("Accept", "application/json")
                .when().get(OPEN_API_CUSTOM_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(X_SHOULD_NOT_RUN, nullValue());
    }
}
