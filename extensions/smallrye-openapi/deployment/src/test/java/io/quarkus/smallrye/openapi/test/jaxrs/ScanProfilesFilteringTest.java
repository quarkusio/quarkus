package io.quarkus.smallrye.openapi.test.jaxrs;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class ScanProfilesFilteringTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(ApiResource.class)
                    .addAsResource(new StringAsset("""
                            # Document with multiple inclusion profiles
                            quarkus.smallrye-openapi.multi.scan-profiles=admin,moderator

                            # Document with exclusion profiles
                            quarkus.smallrye-openapi.public.scan-exclude-profiles=internal,admin

                            # Document with both inclusion and exclusion
                            # Only excluded will have effect
                            quarkus.smallrye-openapi.mixed.scan-profiles=admin,moderator
                            quarkus.smallrye-openapi.mixed.scan-exclude-profiles=deprecated
                            """), "application.properties"));

    @Test
    void testMultipleInclusionProfiles() {
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-multi")
                .then()
                .statusCode(200)
                .body("paths.'/api/admin'", notNullValue())
                .body("paths.'/api/moderator'", notNullValue())
                .body("paths.'/api/public'", nullValue())
                .body("paths.'/api/internal'", nullValue())
                .body("paths.'/api/deprecated'", notNullValue())
                .body("paths.'/api/multi-profile'", notNullValue())
                .body("paths.'/api/no-profile'", nullValue());
    }

    @Test
    void testExclusionProfiles() {
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-public")
                .then()
                .statusCode(200)
                .body("paths.'/api/admin'", nullValue())
                .body("paths.'/api/moderator'", notNullValue())
                .body("paths.'/api/public'", notNullValue())
                .body("paths.'/api/internal'", nullValue())
                .body("paths.'/api/deprecated'", nullValue())
                .body("paths.'/api/multi-profile'", nullValue())
                .body("paths.'/api/no-profile'", notNullValue());
    }

    @Test
    void testMixedInclusionAndExclusion() {
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-mixed")
                .then()
                .statusCode(200)
                .body("paths.'/api/admin'", notNullValue())
                .body("paths.'/api/moderator'", notNullValue())
                .body("paths.'/api/public'", notNullValue())
                .body("paths.'/api/internal'", notNullValue())
                .body("paths.'/api/deprecated'", nullValue())
                .body("paths.'/api/multi-profile'", notNullValue())
                .body("paths.'/api/no-profile'", notNullValue());
    }

    @Test
    void testDefaultDocumentIncludesAllProfiles() {
        // Default document with no filters should include all operations
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("paths.'/api/admin'", notNullValue())
                .body("paths.'/api/moderator'", notNullValue())
                .body("paths.'/api/public'", notNullValue())
                .body("paths.'/api/internal'", notNullValue())
                .body("paths.'/api/deprecated'", notNullValue())
                .body("paths.'/api/multi-profile'", notNullValue())
                .body("paths.'/api/no-profile'", notNullValue());
    }

    @Test
    void testOperationWithNoProfileAppearsCorrectly() {
        // Operations without profiles should appear in default and exclusion-only documents
        // but NOT in documents with inclusion filters

        // Should be in default
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body(containsString("/no-profile"));

        // Should be in public (exclusion-only document)
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-public")
                .then()
                .statusCode(200)
                .body(containsString("/no-profile"));

        // Should NOT be in multi (has inclusion filter)
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-multi")
                .then()
                .statusCode(200)
                .body(not(containsString("/no-profile")));
    }

    @Path("/api")
    public static class ApiResource {

        @GET
        @Path("/admin")
        @Extension(name = "x-smallrye-profile-admin", value = "")
        public String adminEndpoint() {
            return "admin";
        }

        @GET
        @Path("/moderator")
        @Extension(name = "x-smallrye-profile-moderator", value = "")
        public String moderatorEndpoint() {
            return "moderator";
        }

        @GET
        @Path("/public")
        @Extension(name = "x-smallrye-profile-public", value = "")
        public String publicEndpoint() {
            return "public";
        }

        @GET
        @Path("/internal")
        @Extension(name = "x-smallrye-profile-internal", value = "")
        public String internalEndpoint() {
            return "internal";
        }

        @GET
        @Path("/deprecated")
        @Extension(name = "x-smallrye-profile-admin", value = "")
        @Extension(name = "x-smallrye-profile-deprecated", value = "")
        public String deprecatedAdminEndpoint() {
            return "deprecated";
        }

        @GET
        @Path("/multi-profile")
        @Extension(name = "x-smallrye-profile-admin", value = "")
        @Extension(name = "x-smallrye-profile-moderator", value = "")
        public String multiProfileEndpoint() {
            return "multi";
        }

        @GET
        @Path("/no-profile")
        public String noProfileEndpoint() {
            return "no-profile";
        }
    }
}
