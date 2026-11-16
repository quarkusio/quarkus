package io.quarkus.oidc.token.propagation.graphql;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;

public class AccessTokenPropagationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedGraphQLApi.class, PropagationTypesafeGraphQLClient.class, FrontendResource.class))
            .overrideRuntimeConfigKey("quarkus.smallrye-graphql-client.propagation-client.url",
                    "http://localhost:${quarkus.http.test-port:8081}/graphql");

    private final KeycloakTestClient keycloakClient = new KeycloakTestClient();

    @Test
    public void propagatesInboundToken() {
        RestAssured.given().auth().oauth2(keycloakClient.getAccessToken("alice"))
                .when().get("/frontend/me")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
        RestAssured.given().auth().oauth2(keycloakClient.getAccessToken("bob"))
                .when().get("/frontend/me")
                .then()
                .statusCode(200)
                .body(equalTo("bob"));
    }

    @Test
    public void propagatesInboundTokenWhenClientIsAsynchronous() {
        RestAssured.given().auth().oauth2(keycloakClient.getAccessToken("alice"))
                .when().get("/frontend/me-async")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }
}
