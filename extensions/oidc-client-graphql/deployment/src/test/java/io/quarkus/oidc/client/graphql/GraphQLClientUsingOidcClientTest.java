package io.quarkus.oidc.client.graphql;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

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
        // dynamic clients without more specific configuration should always resort to the default
        // OIDC client configured with the `quarkus.oidc-client-graphql.client-name` property
        RestAssured.when().get("/oidc-graphql-client/default-dynamic")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    void dynamicClientsWithExplicitlyAssignedOidcClient() {
        // expect 'jdoe' as we configured 'quarkus.oidc-client-graphql.jdoe-dynamic.client-name=doe'
        RestAssured.when().get("/oidc-graphql-client/jdoe-dynamic")
                .then()
                .statusCode(200)
                .body(equalTo("jdoe"));
        // expect 'admin' as we configured 'quarkus.oidc-client-graphql.admin-dynamic.client-name=admin'
        RestAssured.when().get("/oidc-graphql-client/admin-dynamic")
                .then()
                .statusCode(200)
                .body(equalTo("admin"));
    }

    @Test
    void testTokenNotRenewedOnEveryCall() {
        String accessToken1 = getAccessTokenUsedByGraphQLClient();
        String accessToken2 = getAccessTokenUsedByGraphQLClient();
        assertEquals(accessToken1, accessToken2, "Expected that GraphQL client will reuse the same access token");
    }

    private static String getAccessTokenUsedByGraphQLClient() {
        return RestAssured.when().get("/oidc-graphql-client/access-token")
                .then()
                .statusCode(200)
                .body(Matchers.notNullValue())
                .extract().asString();
    }
}
