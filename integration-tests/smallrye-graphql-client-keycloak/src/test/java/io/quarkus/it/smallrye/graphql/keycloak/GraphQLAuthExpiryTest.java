package io.quarkus.it.smallrye.graphql.keycloak;

import static io.restassured.RestAssured.when;

import java.net.URL;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * See `GraphQLClientTester` for the actual testing code that uses GraphQL clients.
 */
@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class GraphQLAuthExpiryTest {

    @TestHTTPResource
    URL url;

    @Test
    public void testDynamicClientWebSocketAuthenticationExpiry() {
        String token = KeycloakRealmResourceManager.getAccessToken();
        when()
                .get("/dynamic-subscription-auth-expiry/" + token + "/" + url.toString())
                .then()
                .log().everything()
                .statusCode(204);
    }

}
