package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class AutoSecurityDisabledTestDMT {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiWithSecurity.class)
                    .addAsResource(
                            new StringAsset("quarkus.security.auth.enabled-in-dev-mode=false"),
                            "application.properties"));

    @Test
    void testAutoSecurityRequirement() {

        RestAssured.given()
                .header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .log().body()
                .and()
                // Make sure `security` is NOT present
                .body("paths.'/openApiWithSecurity/test-security/annotated'.get.security", nullValue())
                .body("paths.'/openApiWithSecurity/test-security/annotated2'.get.security", nullValue())
                .body("paths.'/openApiWithSecurity/test-security/naked'.get.security", nullValue())
                // Make sure only 200 response is present
                .body("paths.'/openApiWithSecurity/test-security/annotated'.get.responses.size()", equalTo(1))
                .body("paths.'/openApiWithSecurity/test-security/annotated'.get.responses['200']", notNullValue())
                .body("paths.'/openApiWithSecurity/test-security/annotated'.get.responses['401']", nullValue())
                .body("paths.'/openApiWithSecurity/test-security/annotated'.get.responses['403']", nullValue())
                .body("paths.'/openApiWithSecurity/test-security/annotated2'.get.responses.size()", equalTo(1))
                .body("paths.'/openApiWithSecurity/test-security/annotated2'.get.responses['200']", notNullValue())
                .body("paths.'/openApiWithSecurity/test-security/annotated2'.get.responses['401']", nullValue())
                .body("paths.'/openApiWithSecurity/test-security/annotated2'.get.responses['403']", nullValue())
                .body("paths.'/openApiWithSecurity/test-security/naked'.get.responses.size()", equalTo(1))
                .body("paths.'/openApiWithSecurity/test-security/naked'.get.responses['200']", notNullValue())
                .body("paths.'/openApiWithSecurity/test-security/naked'.get.responses['401']", nullValue())
                .body("paths.'/openApiWithSecurity/test-security/naked'.get.responses['403']", nullValue());
    }

}
