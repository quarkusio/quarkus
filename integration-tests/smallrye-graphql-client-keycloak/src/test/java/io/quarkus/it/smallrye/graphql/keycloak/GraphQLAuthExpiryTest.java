package io.quarkus.it.smallrye.graphql.keycloak;

import static io.restassured.RestAssured.when;

import java.net.URL;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.smallrye.graphql.client.websocket.WebsocketSubprotocol;

/**
 * See `GraphQLClientTester` for the actual testing code that uses GraphQL clients.
 */
@QuarkusTest
@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class GraphQLAuthExpiryTest {

    @TestHTTPResource
    URL url;

    final KeycloakTestClient client = new KeycloakTestClient();

    private static Stream<Arguments> clientOptions() {
        Boolean[] clientInitValues = new Boolean[] { true, false };
        WebsocketSubprotocol[] subprotocolsValues = WebsocketSubprotocol.values();

        return Stream.of(clientInitValues).flatMap(
                clientInit -> Stream.of(subprotocolsValues).map(subprotocol -> Arguments.of(clientInit, subprotocol.name())));
    }

    @ParameterizedTest
    @MethodSource("clientOptions")
    public void testDynamicClientWebSocketAuthenticationExpiry(boolean clientInit, String subprotocol) {
        String token = client.getAccessToken("alice");
        when()
                .get("/dynamic-subscription-auth-expiry/" + clientInit + "/" + subprotocol + "/" + token + "/" + url.toString())
                .then()
                .log().everything()
                .statusCode(204);
    }

}
