package io.quarkus.smallrye.graphql.client.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.security.RolesAllowed;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.graphql.api.Subscription;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;
import io.smallrye.mutiny.Multi;

/**
 * This test establishes connections to the server, and ensures that if authentication has an expiry, that following the
 * expiry of their access the connection is correctly terminated.
 */
@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class DynamicGraphQLClientWebSocketAuthenticationExpiryTest {

    static String url = "http://" + System.getProperty("quarkus.http.host", "localhost") + ":" +
            System.getProperty("quarkus.http.test-port", "8081") + "/graphql";

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String keycloakRealm;

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredApi.class, TestResponse.class)
                    .addAsResource(new StringAsset("quarkus.oidc.client-id=graphql-test\n" +
                            "quarkus.oidc.credentials.secret=secret\n" +
                            "quarkus.smallrye-graphql.log-payload=queryAndVariables"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testAuthenticatedUserWithExpiryForSubscription() throws Exception {
        String authHeader = getAuthHeader();

        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url)
                .header("Authorization", authHeader)
                .executeSingleOperationsOverWebsocket(true);

        try (DynamicGraphQLClient client = clientBuilder.build()) {
            AtomicBoolean ended = new AtomicBoolean(false);
            AtomicBoolean receivedValue = new AtomicBoolean(false);
            client.subscription("subscription { sub { value } }").subscribe().with(item -> {
                assertTrue(item.hasData());
                receivedValue.set(true);
            }, Assertions::fail, () -> {
                // Set to true to notify the test that the Auth token has expired.
                ended.set(true);
            });
            await().untilTrue(ended);
            assertTrue(receivedValue.get());
        }
    }

    private String getAuthHeader() {
        io.restassured.response.Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .formParam("username", "alice")
                .formParam("password", "alice")
                .param("client_id", "quarkus-service-app")
                .param("client_secret", "secret")
                .formParam("grant_type", "password")
                .post(keycloakRealm + "/protocol/openid-connect/token");

        return "Bearer " + response.getBody().jsonPath().getString("access_token");
    }

    public static class TestResponse {

        private final String value;

        public TestResponse(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @GraphQLApi
    public static class SecuredApi {

        // Seems to be a bug with SmallRye GraphQL which requires you to have a query or mutation in a GraphQLApi.
        // This is a workaround for the time being.
        @Query
        public TestResponse unusedQuery() {
            return null;
        }

        @Subscription
        @RolesAllowed("user")
        @NonBlocking
        public Multi<TestResponse> sub() {
            return Multi.createFrom().emitter(emitter -> emitter.emit(new TestResponse("Hello World")));
        }
    }
}
