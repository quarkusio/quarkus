package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class ApiKeySecurityWithConfigTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(OpenApiResource.class, ResourceBean.class)
            .addAsResource(new StringAsset("quarkus.smallrye-openapi.security-scheme=apiKey\n"
                    + "quarkus.smallrye-openapi.security-scheme-name=APIKeyCompanyAuthentication\n"
                    + "quarkus.smallrye-openapi.security-scheme-description=API Key Authentication\n"
                    + "quarkus.smallrye-openapi.api-key-parameter-in=cookie\n"
                    + "quarkus.smallrye-openapi.api-key-parameter-name=APIKEY"), "application.properties"));

    @Test
    void testApiKeyAuthentication() {
        RestAssured.given().header("Accept", "application/json").when().get("/q/openapi").then().body(
                "components.securitySchemes.APIKeyCompanyAuthentication",
                allOf(hasEntry("type", "apiKey"), hasEntry("description", "API Key Authentication"),
                        hasEntry("in", "cookie"), hasEntry("name", "APIKEY")));
    }
}
