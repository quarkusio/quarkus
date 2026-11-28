package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;

public class AccessTokenPropagationDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(PublicGraphQLApi.class, DevModeTypesafeGraphQLClient.class, DevModeFrontendResource.class)
                    .addAsResource(
                            new StringAsset(
                                    "quarkus.smallrye-graphql-client.dev-mode-client.url=http://localhost:${quarkus.http.port:8080}/graphql\n"),
                            "application.properties"));

    @Test
    public void testAccessTokenAnnotationRemovedAndAdded() {
        String authServerUrl = RestAssured.when().get("/dev-mode-frontend/auth-server-url")
                .then().statusCode(200).extract().asString();
        String token = new KeycloakTestClient(authServerUrl).getAccessToken("alice");
        assertPropagatedIdentity(token, "alice");

        // the access token must not be propagated anymore
        test.modifySourceFile(DevModeTypesafeGraphQLClient.class, s -> s.replace("@AccessToken", ""));
        assertPropagatedIdentity(token, "anonymous");

        test.modifySourceFile(DevModeTypesafeGraphQLClient.class,
                s -> s.replace("public interface", "@AccessToken public interface"));
        assertPropagatedIdentity(token, "alice");
    }

    private static void assertPropagatedIdentity(String token, String identity) {
        RestAssured.given().auth().oauth2(token)
                .when().get("/dev-mode-frontend/me")
                .then()
                .statusCode(200)
                .body(equalTo(identity));
    }
}
