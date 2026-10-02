package io.quarkus.it.panache.reactive.sessionrestart;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(SessionFactoryRestart01ResourceTest.SessionFactoryRestartProfile.class)
public class SessionFactoryRestart02NoResourceTest {

    @Test
    public void testPanacheSessionAfterRestart() {
        RestAssured.when().get("/test-transactional/panache-session")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }
}
