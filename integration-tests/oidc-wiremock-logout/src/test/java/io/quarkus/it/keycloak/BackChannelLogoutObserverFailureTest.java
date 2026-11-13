package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
public class BackChannelLogoutObserverFailureTest {

    @Test
    public void testThrowingObserverDoesNotHangBackChannelLogout() throws Exception {
        CompletableFuture<Integer> status = CompletableFuture.supplyAsync(() -> RestAssured.given()
                .when().contentType(ContentType.URLENC)
                .body("logout_token=" + OidcWiremockTestResource.getLogoutToken(ThrowingBackChannelLogoutObserver.SUBJECT))
                .post("http://localhost:8081/service/back-channel-logout")
                .statusCode());
        try {
            assertEquals(400, status.get(10, TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            fail("The back-channel logout request hung after a synchronous SecurityEvent observer threw");
        }
    }
}
