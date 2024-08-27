package io.quarkus.resteasy.reactive.server.test.security.authzpolicy;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class LazyAuthAuthorizationPolicyTest extends AbstractAuthorizationPolicyTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TEST_CLASSES)
                    .addClass(BasicAuthenticationResource.class)
                    .addAsResource(new StringAsset("quarkus.http.auth.proactive=false\n" + APPLICATION_PROPERTIES),
                            "application.properties"));

    @Test
    public void testBasicAuthSelectedWithAnnotation() {
        // no @AuthorizationPolicy == authentication required
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/basic-auth-ann")
                .then().statusCode(200).body(Matchers.equalTo("admin"));
        RestAssured.given().auth().preemptive().basic("viewer", "viewer").get("/basic-auth-ann")
                .then().statusCode(200).body(Matchers.equalTo("viewer"));
        RestAssured.given().get("/basic-auth-ann").then().statusCode(401);

        // @AuthorizationPolicy requires viewer and overrides class level @BasicAuthentication
        RestAssured.given().auth().preemptive().basic("admin", "admin").get("/basic-auth-ann/authorization-policy")
                .then().statusCode(403);
        RestAssured.given().auth().preemptive().basic("viewer", "viewer").get("/basic-auth-ann/authorization-policy")
                .then().statusCode(200).body(Matchers.equalTo("viewer"));
        RestAssured.given().get("/basic-auth-ann/authorization-policy").then().statusCode(401);
    }
}
