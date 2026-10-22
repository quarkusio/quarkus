package io.quarkus.oidc.token.propagation.graphql;

import static org.hamcrest.Matchers.equalTo;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;
import io.smallrye.config.Config;

public class AccessTokenPropagationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedGraphQLApi.class, PropagationTypesafeGraphQLClient.class, FrontendResource.class)
                    .addAsResource(
                            new StringAsset(
                                    """
                                            quarkus.smallrye-graphql-client.propagation-client.url=http://localhost:${quarkus.http.test-port}/graphql
                                            """),
                            "application.properties"));

    @Inject
    Config config;

    @Test
    public void propagatesInboundToken() {
        System.out.println(config.getConfigValue("quarkus.oidc.auth-server-url"));
        KeycloakTestClient keycloakClient = new KeycloakTestClient(
                config.getConfigValue("quarkus.oidc.auth-server-url").getValue());
        String token = keycloakClient.getAccessToken("alice");
        RestAssured.given().auth().oauth2(token)
                .when().get("/frontend/me")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }
}
