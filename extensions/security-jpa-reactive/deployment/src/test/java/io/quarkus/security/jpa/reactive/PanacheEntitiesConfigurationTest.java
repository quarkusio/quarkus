package io.quarkus.security.jpa.reactive;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.NonUniqueResultException;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
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
                    .addClass(AuthenticationFailureObserver.class)
                    .addAsResource("multiple-entities/import.sql", "import.sql")
                    .addAsResource("multiple-entities/application.properties", "application.properties"));

    @Inject
    AuthenticationFailureObserver authenticationFailureObserver;

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
        authenticationFailureObserver.recordEvents(true);
        getUsername().statusCode(401).body(Matchers.emptyOrNullString());
        Awaitility.await().untilAsserted(() -> assertNotNull(authenticationFailureObserver.getEvent()));
        var authFailureEvent = authenticationFailureObserver.getEvent();
        assertInstanceOf(AuthenticationFailedException.class, authFailureEvent.getAuthenticationFailure());
        var cause = authFailureEvent.getAuthenticationFailure().getCause();
        assertInstanceOf(NonUniqueResultException.class, cause);
        assertTrue(cause.getMessage().contains("Query did not return a unique result"));
        authenticationFailureObserver.recordEvents(false);
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

    @Singleton
    public static class AuthenticationFailureObserver {

        private volatile boolean record = false;
        private volatile AuthenticationFailureEvent event;

        void observerAuthFailure(@Observes AuthenticationFailureEvent event) {
            if (record) {
                this.event = event;
            }
        }

        void recordEvents(boolean record) {
            this.record = record;
            if (!record) {
                this.event = null;
            }
        }

        AuthenticationFailureEvent getEvent() {
            return event;
        }
    }
}
