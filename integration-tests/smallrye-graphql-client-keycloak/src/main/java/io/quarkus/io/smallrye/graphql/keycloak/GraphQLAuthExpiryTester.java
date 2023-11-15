package io.quarkus.io.smallrye.graphql.keycloak;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;

/**
 * We can't perform these tests in the `@Test` methods directly, because the GraphQL client
 * relies on CDI, and CDI is not available in native mode on the `@Test` side.
 * Therefore the test only calls this REST endpoint which then performs all the client related work.
 * <br>
 * This test establishes connections to the server, and ensures that if authentication has an expiry, that following the
 * expiry of their access the connection is correctly terminated.
 */
@Path("/")
public class GraphQLAuthExpiryTester {

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String keycloakRealm;

    @GET
    @Path("/dynamic-subscription-auth-expiry/{url}")
    @Blocking
    public void dynamicSubscription(@PathParam("url") String url)
            throws Exception {
        String authHeader = getAuthHeader();

        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url + "/graphql")
                .header("Authorization", authHeader)
                .executeSingleOperationsOverWebsocket(true);

        try (DynamicGraphQLClient client = clientBuilder.build()) {
            CompletableFuture<Void> authenticationExpired = new CompletableFuture<>();
            AtomicBoolean receivedValue = new AtomicBoolean(false);
            client.subscription("subscription { sub { value } }").subscribe().with(item -> {
                if (item.hasData()) {
                    receivedValue.set(true);
                } else {
                    authenticationExpired.completeExceptionally(new RuntimeException("Subscription provided no data"));
                }
            }, cause -> {
                if (cause.getMessage().contains("Authentication expired")) {
                    authenticationExpired.complete(null);
                } else {
                    authenticationExpired
                            .completeExceptionally(new RuntimeException("Invalid close response from server.", cause));
                }
            }, () -> authenticationExpired
                    .completeExceptionally(new RuntimeException("Subscription should not complete successfully")));

            authenticationExpired.get(10, TimeUnit.SECONDS);
            if (!receivedValue.get()) {
                throw new RuntimeException("Did not receive subscription value");
            }
        }
    }

    private String getAuthHeader() {
        io.restassured.response.Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .formParam("username", "alice")
                .formParam("password", "alice")
                .param("client_id", "quarkus-app")
                .param("client_secret", "secret")
                .formParam("grant_type", "password")
                .post(keycloakRealm + "/protocol/openid-connect/token");

        return "Bearer " + response.getBody().jsonPath().getString("access_token");
    }
}
