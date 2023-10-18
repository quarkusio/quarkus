package io.quarkus.oidc.client.graphql;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class GraphQLClientUsingOidcClientTest {

    private static final Class<?>[] testClasses = {
            ProtectedResource.class,
            GraphQLClientResource.class,
            AnnotationTypesafeGraphQLClient.class,
            DefaultTypesafeGraphQLClient.class
    };

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application.properties", "application.properties"));

    @Test
    public void typesafeClientAnnotation() {
        // OidcClient selected via @OidcClient("clientName")
        RestAssured.when().get("/oidc-graphql-client/annotation-typesafe")
                .then()
                .statusCode(200)
                .body(equalTo("jdoe"));
    }

    @Test
    public void typesafeClientWithDefault() {
        // OidcClient selected via @OidcClient without a value - this should resort to the default client
        RestAssured.when().get("/oidc-graphql-client/default-typesafe")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
        // just to make sure it works more than once
        RestAssured.when().get("/oidc-graphql-client/default-typesafe")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    public void dynamicClientWithDefault() {
        // dynamic clients should always resort to the default (`quarkus.oidc-client-graphql.client-name`),
        // because currently we don't have a way to override it
        RestAssured.when().get("/oidc-graphql-client/default-dynamic")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

}
