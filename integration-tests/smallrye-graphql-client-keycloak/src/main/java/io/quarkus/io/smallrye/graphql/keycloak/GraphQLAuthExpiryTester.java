package io.quarkus.io.smallrye.graphql.keycloak;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

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

    @GET
    @Path("/dynamic-subscription-auth-expiry/{token}/{url}")
    @Blocking
    public void dynamicSubscription(@PathParam("token") String token, @PathParam("url") String url)
            throws Exception {
        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url + "/graphql")
                .header("Authorization", "Bearer " + token)
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

}
