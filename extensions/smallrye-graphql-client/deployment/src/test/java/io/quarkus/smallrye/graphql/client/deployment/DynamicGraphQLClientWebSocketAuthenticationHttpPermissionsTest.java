package io.quarkus.smallrye.graphql.client.deployment;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.graphql.api.Subscription;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.core.http.UpgradeRejectedException;

public class DynamicGraphQLClientWebSocketAuthenticationHttpPermissionsTest {

    static String url = "http://" + System.getProperty("quarkus.http.host", "localhost") + ":" +
            System.getProperty("quarkus.http.test-port", "8081") + "/graphql";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredApi.class, Foo.class)
                    .addAsResource("application-secured-http-permissions.properties", "application.properties")
                    .addAsResource("users.properties")
                    .addAsResource("roles.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testUnauthenticatedForQueryWebSocket() throws Exception {
        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url)
                .executeSingleOperationsOverWebsocket(true);
        try (DynamicGraphQLClient client = clientBuilder.build()) {
            try {
                client.executeSync("{ baz { message} }");
                Assertions.fail("WebSocket upgrade should fail");
            } catch (UpgradeRejectedException e) {
                // ok
            }
        }
    }

    @Test
    public void testUnauthenticatedForSubscriptionWebSocket() throws Exception {
        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url);
        try (DynamicGraphQLClient client = clientBuilder.build()) {
            AssertSubscriber<Response> subscriber = new AssertSubscriber<>();
            client.subscription("{ bazSub { message} }").subscribe().withSubscriber(subscriber);
            subscriber.awaitFailure().assertFailedWith(UpgradeRejectedException.class);
        }
    }

    public static class Foo {

        private String message;

        public Foo(String foo) {
            this.message = foo;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

    }

    @GraphQLApi
    public static class SecuredApi {

        @Query
        public Foo baz() {
            return new Foo("baz");
        }

        @Subscription
        public Multi<Foo> bazSub() {
            return Multi.createFrom().emitter(emitter -> {
                emitter.emit(new Foo("baz"));
                emitter.complete();
            });
        }

    }
}
