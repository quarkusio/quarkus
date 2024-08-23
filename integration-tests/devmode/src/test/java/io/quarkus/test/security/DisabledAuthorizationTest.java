package io.quarkus.test.security;

import static org.hamcrest.CoreMatchers.equalTo;

import jakarta.annotation.security.DenyAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DisabledAuthorizationTest {
    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class)
                    .add(new StringAsset("quarkus.security.auth.enabled-in-dev-mode=false"), "application.properties"));

    @Test
    void verifyAuthorizationEnablement() {
        RestAssured.given()
                .when().get("/")
                .then()
                .statusCode(200)
                .body(equalTo("hello"));
    }

    @ApplicationScoped
    @Path("/")
    @DenyAll
    public static class HelloResource {
        @GET
        public String hello() {
            return "hello";
        }
    }
}
