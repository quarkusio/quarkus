package io.quarkus.security.jpa.reactive;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

public class PanacheEntitiesConfigurationTest extends JpaSecurityRealmTest {

    private static final String DUPLICATE_USERNAME = "merlin";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addClass(PanacheUserEntity.class)
                    .addClass(PanacheRoleEntity.class)
                    .addClass(UserResource.class)
                    .addAsResource("multiple-entities/import.sql", "import.sql")
                    .addAsResource("multiple-entities/application.properties", "application.properties"));

    @Test
    void duplicateUsernameTest() {
        // duplicate username must lead to 401
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // no user -> unauthenticated
        getUsername().statusCode(401);
        createUser();
        // one user
        getUsername().statusCode(200).body(is(DUPLICATE_USERNAME));
        createUser();
        // two users -> NonUniqueResultException -> 401
        getUsername().statusCode(401);
    }

    private static void createUser() {
        RestAssured
                .given()
                .auth().preemptive().basic("user", "user")
                .body(DUPLICATE_USERNAME)
                .post("/jaxrs-secured/user")
                .then()
                .statusCode(201);
    }

    private static ValidatableResponse getUsername() {
        return RestAssured
                .given()
                .auth().preemptive().basic(DUPLICATE_USERNAME, DUPLICATE_USERNAME)
                .body(DUPLICATE_USERNAME)
                .get("/jaxrs-secured/user")
                .then();
    }

}
