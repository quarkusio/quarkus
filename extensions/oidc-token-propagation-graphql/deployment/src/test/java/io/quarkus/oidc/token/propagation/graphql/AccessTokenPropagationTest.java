package io.quarkus.oidc.token.propagation.graphql;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class AccessTokenPropagationTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedGraphQLApi.class, PropagationTypesafeGraphQLClient.class, FrontendResource.class)
                    .addAsResource("application.properties"));

    KeycloakTestClient keycloakClient = new KeycloakTestClient();

    @Test
    public void propagatesInboundToken() {
        String token = keycloakClient.getAccessToken("alice");
        RestAssured.given().auth().oauth2(token)
                .when().get("/frontend/me")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }
}
