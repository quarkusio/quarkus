package io.quarkus.test.security;

import static org.hamcrest.CoreMatchers.equalTo;

import javax.annotation.security.DenyAll;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
