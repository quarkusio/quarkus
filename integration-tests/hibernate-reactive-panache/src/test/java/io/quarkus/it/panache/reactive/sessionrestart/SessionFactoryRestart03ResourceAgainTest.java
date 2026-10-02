package io.quarkus.it.panache.reactive.sessionrestart;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.LogCollectingTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(SessionFactoryRestart01ResourceTest.SessionFactoryRestartProfile.class)
@QuarkusTestResource(value = LogCollectingTestResource.class, restrictToAnnotatedClass = true)
public class SessionFactoryRestart03ResourceAgainTest {

    @Test
    public void testPanacheSessionAfterSecondRestart() {
        RestAssured.when().get("/test-transactional/panache-session")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }
}
