package io.quarkus.smallrye.openapi.test.jaxrs;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.test.QuarkusUnitTest;

class FilterDocumentScopingTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(TestResource.class, DefaultAndUserFilter.class,
                            OnlyUserFilter.class, AllDocumentsFilter.class,
                            OnlyDefaultFilter.class)
                    .addAsResource(new StringAsset("""
                            quarkus.smallrye-openapi.info-title=Default API

                            quarkus.smallrye-openapi.user.scan-profiles=user
                            quarkus.smallrye-openapi.user.info-title=User API

                            quarkus.smallrye-openapi.admin.scan-profiles=admin
                            quarkus.smallrye-openapi.admin.info-title=Admin API
                            """), "application.properties"));

    @Test
    void testDefaultAndUserFilterAppliesOnlyToDefaultAndUser() {
        // Should be in default document
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("x-default-user-filter", is("applied"));

        // Should be in user document
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-user")
                .then()
                .statusCode(200)
                .body("x-default-user-filter", is("applied"));

        // Should NOT be in admin document
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-admin")
                .then()
                .statusCode(200)
                .body("x-default-user-filter", nullValue());
    }

    @Test
    void testOnlyUserFilterAppliesOnlyToUser() {
        // Should NOT be in default document
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("x-only-user-filter", nullValue());

        // Should be in user document
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-user")
                .then()
                .statusCode(200)
                .body("x-only-user-filter", is("applied"));

        // Should NOT be in admin document
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-admin")
                .then()
                .statusCode(200)
                .body("x-only-user-filter", nullValue());
    }

    @Test
    void testAllDocumentsFilterAppliesEverywhere() {
        // Should be in default document
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("x-all-docs-filter", is("applied"));

        // Should be in user document
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-user")
                .then()
                .statusCode(200)
                .body("x-all-docs-filter", is("applied"));

        // Should be in admin document
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-admin")
                .then()
                .statusCode(200)
                .body("x-all-docs-filter", is("applied"));
    }

    @Test
    void testOnlyDefaultFilterAppliesOnlyToDefault() {
        // Should be in default document
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("x-only-default-filter", is("applied"));

        // Should NOT be in user document
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-user")
                .then()
                .statusCode(200)
                .body("x-only-default-filter", nullValue());

        // Should NOT be in admin document
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-admin")
                .then()
                .statusCode(200)
                .body("x-only-default-filter", nullValue());
    }

    @Test
    void testMultipleFiltersCanApplyToSameDocument() {
        // User document should have all applicable filters
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-user")
                .then()
                .statusCode(200)
                .body("x-default-user-filter", is("applied"))
                .body("x-only-user-filter", is("applied"))
                .body("x-all-docs-filter", is("applied"))
                .body("x-only-default-filter", nullValue());
    }

    @Path("/api")
    public static class TestResource {
        @GET
        @Path("/user")
        @Extension(name = "x-smallrye-profile-user", value = "")
        public String userEndpoint() {
            return "user";
        }

        @GET
        @Path("/admin")
        @Extension(name = "x-smallrye-profile-admin", value = "")
        public String adminEndpoint() {
            return "admin";
        }
    }

    @OpenApiFilter(documentNames = { OpenApiFilter.DEFAULT_DOCUMENT_NAME, "user" })
    public static class DefaultAndUserFilter implements OASFilter {
        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            openAPI.addExtension("x-default-user-filter", "applied");
        }
    }

    @OpenApiFilter(documentNames = { "user" })
    public static class OnlyUserFilter implements OASFilter {
        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            openAPI.addExtension("x-only-user-filter", "applied");
        }
    }

    @OpenApiFilter(documentNames = { OpenApiFilter.FILTER_RUN_FOR_ANY_DOCUMENT })
    public static class AllDocumentsFilter implements OASFilter {
        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            openAPI.addExtension("x-all-docs-filter", "applied");
        }
    }

    @OpenApiFilter(documentNames = { OpenApiFilter.DEFAULT_DOCUMENT_NAME })
    public static class OnlyDefaultFilter implements OASFilter {
        @Override
        public void filterOpenAPI(OpenAPI openAPI) {
            openAPI.addExtension("x-only-default-filter", "applied");
        }
    }
}
