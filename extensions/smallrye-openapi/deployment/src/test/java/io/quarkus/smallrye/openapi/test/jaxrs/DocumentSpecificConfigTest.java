package io.quarkus.smallrye.openapi.test.jaxrs;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class DocumentSpecificConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(TestResource.class)
                    .addAsResource(new StringAsset("""
                            # Default document config
                            quarkus.smallrye-openapi.info-title=Default API
                            quarkus.smallrye-openapi.info-version=1.0.0
                            quarkus.smallrye-openapi.info-description=Default API Description
                            quarkus.smallrye-openapi.info-contact-name=Default Team
                            quarkus.smallrye-openapi.info-contact-email=default@example.com
                            quarkus.smallrye-openapi.servers=https://default.example.com

                            # V1 document config
                            quarkus.smallrye-openapi.v1.scan-profiles=v1
                            quarkus.smallrye-openapi.v1.info-title=API v1
                            quarkus.smallrye-openapi.v1.info-version=1.0.0
                            quarkus.smallrye-openapi.v1.info-description=Version 1 API
                            quarkus.smallrye-openapi.v1.info-contact-name=V1 Team
                            quarkus.smallrye-openapi.v1.info-contact-email=v1@example.com
                            quarkus.smallrye-openapi.v1.servers=https://v1.example.com,https://v1-staging.example.com

                            # V2 document config
                            quarkus.smallrye-openapi.v2.scan-profiles=v2
                            quarkus.smallrye-openapi.v2.info-title=API v2
                            quarkus.smallrye-openapi.v2.info-version=2.0.0
                            quarkus.smallrye-openapi.v2.info-description=Version 2 API
                            quarkus.smallrye-openapi.v2.info-contact-name=V2 Team
                            quarkus.smallrye-openapi.v2.info-contact-email=v2@example.com
                            quarkus.smallrye-openapi.v2.info-terms-of-service=https://v2.example.com/terms
                            quarkus.smallrye-openapi.v2.servers=https://v2.example.com

                            # Internal document config with different settings
                            quarkus.smallrye-openapi.internal.scan-profiles=internal
                            quarkus.smallrye-openapi.internal.info-title=Internal API
                            quarkus.smallrye-openapi.internal.info-version=internal-1.0
                            quarkus.smallrye-openapi.internal.info-license-name=Proprietary
                            quarkus.smallrye-openapi.internal.info-license-url=https://internal.example.com/license
                            quarkus.smallrye-openapi.internal.auto-add-tags=false
                            quarkus.smallrye-openapi.internal.auto-add-operation-summary=false
                            """), "application.properties"));

    @Test
    void testDefaultDocumentHasCorrectInfo() {
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("info.title", is("Default API"))
                .body("info.version", is("1.0.0"))
                .body("info.description", is("Default API Description"))
                .body("info.contact.name", is("Default Team"))
                .body("info.contact.email", is("default@example.com"))
                .body("servers[0].url", is("https://default.example.com"));
    }

    @Test
    void testV1DocumentHasCorrectInfo() {
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-v1")
                .then()
                .statusCode(200)
                .body("info.title", is("API v1"))
                .body("info.version", is("1.0.0"))
                .body("info.description", is("Version 1 API"))
                .body("info.contact.name", is("V1 Team"))
                .body("info.contact.email", is("v1@example.com"))
                .body("servers[0].url", is("https://v1.example.com"))
                .body("servers[1].url", is("https://v1-staging.example.com"));
    }

    @Test
    void testV2DocumentHasCorrectInfo() {
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-v2")
                .then()
                .statusCode(200)
                .body("info.title", is("API v2"))
                .body("info.version", is("2.0.0"))
                .body("info.description", is("Version 2 API"))
                .body("info.contact.name", is("V2 Team"))
                .body("info.contact.email", is("v2@example.com"))
                .body("info.termsOfService", is("https://v2.example.com/terms"))
                .body("servers[0].url", is("https://v2.example.com"))
                .body(not(containsString("https://v1.example.com")))
                .body(not(containsString("Default Team")));
    }

    @Test
    void testInternalDocumentHasCorrectInfo() {
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-internal")
                .then()
                .statusCode(200)
                .body("info.title", is("Internal API"))
                .body("info.version", is("internal-1.0"))
                .body("info.license.name", is("Proprietary"))
                .body("info.license.url", is("https://internal.example.com/license"))
                .body(not(containsString("Default API")))
                .body(not(containsString("API v1")));
    }

    @Test
    void testV1ServersDontAppearInV2() {
        String v2Doc = given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-v2")
                .then()
                .statusCode(200)
                .extract().asString();

        // V2 should only have v2 servers, not v1 or default servers
        assertTrue(v2Doc.contains("https://v2.example.com"));
        assertFalse(v2Doc.contains("https://v1.example.com"));
        assertFalse(v2Doc.contains("https://default.example.com"));
    }

    @Test
    void testAutoAddSettingsAreIsolated() {
        // V1 document should have auto-add-tags and auto-add-operation-summary enabled (default behavior)
        String v1Doc = given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-v1")
                .then()
                .statusCode(200)
                .body("paths.'/api/v1/endpoint'.get.tags[0]", is("Test Resource"))
                .body("paths.'/api/v1/endpoint'.get.summary", is("V 1 Endpoint"))
                .extract().asString();

        // Verify v1 document has auto-generated tags and summary
        assertTrue(v1Doc.contains("Test Resource"));
        assertTrue(v1Doc.contains("V 1 Endpoint"));

        // Internal document has auto-add-tags and auto-add-operation-summary disabled
        // So it should NOT have auto-generated tags or summaries
        String internalDoc = given()
                .header("Accept", "application/json")
                .when().get("/q/openapi-internal")
                .then()
                .statusCode(200)
                .extract().asString();

        // Verify internal document does NOT have auto-generated tags or summary
        assertFalse(internalDoc.contains("Test Resource"), "Internal doc should not have auto-added tag 'Test Resource'");
        assertFalse(internalDoc.contains("Internal Endpoint"), "Internal doc should not have auto-added summary");
    }

    @Path("/api")
    static class TestResource {

        @GET
        @Path("/v1/endpoint")
        @Extension(name = "x-smallrye-profile-v1", value = "")
        public String v1Endpoint() {
            return "v1";
        }

        @GET
        @Path("/v2/endpoint")
        @Extension(name = "x-smallrye-profile-v2", value = "")
        public String v2Endpoint() {
            return "v2";
        }

        @GET
        @Path("/internal/endpoint")
        @Extension(name = "x-smallrye-profile-internal", value = "")
        public String internalEndpoint() {
            return "internal";
        }
    }
}
