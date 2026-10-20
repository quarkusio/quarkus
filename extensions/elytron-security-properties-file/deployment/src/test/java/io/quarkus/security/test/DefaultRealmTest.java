package io.quarkus.security.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Tests that the realm named 'Quarkus', here the properties files realm, is the default realm when multiple realms are
 * enabled and the default realm name is not configured.
 */
class DefaultRealmTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestApplication.class, SubjectExposingResource.class)
                    .addAsResource("application-multiple-realms.properties", "application.properties")
                    .addAsResource("test-users.properties")
                    .addAsResource("test-roles.properties"));

    @Test
    void testDefaultRealmAuthenticatesUser() {
        RestAssured.given().auth().preemptive().basic("scott", "jb0ss")
                .when().get("/jaxrs-secured/subject/secured").then()
                .statusCode(200)
                .body(equalTo("scott"));
    }

    @Test
    void testOtherRealmDoesNotAuthenticateUser() {
        RestAssured.given().auth().preemptive().basic("emma", "emma-pwd")
                .when().get("/jaxrs-secured/subject/secured").then()
                .statusCode(401);
    }
}
