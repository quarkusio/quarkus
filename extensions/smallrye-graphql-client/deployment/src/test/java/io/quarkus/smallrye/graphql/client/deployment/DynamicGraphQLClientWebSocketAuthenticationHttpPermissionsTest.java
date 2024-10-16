package io.quarkus.smallrye.graphql.client.deployment;

import jakarta.annotation.security.RolesAllowed;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.graphql.api.Subscription;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.core.http.UpgradeRejectedException;

/**
 * Due to the complexity of establishing a WebSocket, WebSocket/Subscription testing of the GraphQL server is done here,
 * as the client framework comes in very useful for establishing the connection to the server.
 * <br>
 * This test establishes connections to the server, and ensures that the connected user has the necessary permissions to
 * execute the operation.
 */
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

    @Disabled("TODO: enable after upgrade to smallrye-graphql 1.6.1, with 1.6.0 a websocket upgrade failure causes a hang here")
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
        @RolesAllowed("fooRole")
        @NonBlocking
        public Foo foo() {
            return new Foo("foo");
        }

        @Query
        @RolesAllowed("barRole")
        public Foo bar() {
            return new Foo("bar");
        }

        @Query
        public Foo baz() {
            return new Foo("baz");
        }

        @Subscription
        @RolesAllowed("fooRole")
        public Multi<Foo> fooSub() {
            return Multi.createFrom().emitter(emitter -> {
                emitter.emit(new Foo("foo"));
                emitter.complete();
            });
        }

        @Subscription
        @RolesAllowed("barRole")
        public Multi<Foo> barSub() {
            return Multi.createFrom().emitter(emitter -> {
                emitter.emit(new Foo("bar"));
                emitter.complete();
            });
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
