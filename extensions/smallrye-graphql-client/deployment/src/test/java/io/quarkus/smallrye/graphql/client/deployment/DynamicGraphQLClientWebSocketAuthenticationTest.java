package io.quarkus.smallrye.graphql.client.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import io.smallrye.common.annotation.NonBlocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonValue;

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

/**
 * Due to the complexity of establishing a WebSocket, WebSocket/Subscription testing of the GraphQL server is done here,
 * as the client framework comes in very useful for establishing the connection to the server.
 * <br>
 * This test establishes connections to the server, and ensures that the connected user has the necessary permissions to
 * execute the operation.
 */
public class DynamicGraphQLClientWebSocketAuthenticationTest {

    static String url = "http://" + System.getProperty("quarkus.http.host", "localhost") + ":" +
            System.getProperty("quarkus.http.test-port", "8081") + "/graphql";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredApi.class, Foo.class)
                    .addAsResource("application-secured.properties", "application.properties")
                    .addAsResource("users.properties")
                    .addAsResource("roles.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testAuthenticatedUserForSubscription() throws Exception {
        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url)
                .header("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz");
        try (DynamicGraphQLClient client = clientBuilder.build()) {
            Multi<Response> subscription = client
                    .subscription("subscription fooSub { fooSub { message } }");

            assertNotNull(subscription);

            AtomicBoolean hasData = new AtomicBoolean(false);
            AtomicBoolean hasCompleted = new AtomicBoolean(false);

            subscription.subscribe().with(item -> {
                assertFalse(hasData.get());
                assertTrue(item.hasData());
                assertEquals(JsonValue.ValueType.OBJECT, item.getData().get("fooSub").getValueType());
                assertEquals("foo", item.getData().getJsonObject("fooSub").getString("message"));
                hasData.set(true);
            }, Assertions::fail, () -> {
                hasCompleted.set(true);
            });

            await().untilTrue(hasCompleted);
            assertTrue(hasData.get());
        }
    }

    @Test
    public void testAuthenticatedUserForQueryWebSocket() throws Exception {
        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url)
                .header("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz")
                .executeSingleOperationsOverWebsocket(true);
        try (DynamicGraphQLClient client = clientBuilder.build()) {
            Response response = client.executeSync("{ foo { message} }");
            assertTrue(response.hasData());
            assertEquals("foo", response.getData().getJsonObject("foo").getString("message"));
        }
    }

    @Test
    public void testAuthorizedAndUnauthorizedForQueryWebSocket() throws Exception {
        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url)
                .header("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz")
                .executeSingleOperationsOverWebsocket(true);
        try (DynamicGraphQLClient client = clientBuilder.build()) {
            Response response = client.executeSync("{ foo { message} }");
            assertTrue(response.hasData());
            assertEquals("foo", response.getData().getJsonObject("foo").getString("message"));

            // Run a second query with a different result to validate that the result of the first query isn't being cached at all.
            response = client.executeSync("{ bar { message} }");
            assertEquals(JsonValue.ValueType.NULL, response.getData().get("bar").getValueType());
        }
    }

    @Test
    public void testUnauthorizedUserForSubscription() throws Exception {
        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url)
                .header("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz");
        try (DynamicGraphQLClient client = clientBuilder.build()) {
            Multi<Response> subscription = client
                    .subscription("subscription barSub { barSub { message } }");

            assertNotNull(subscription);

            AtomicBoolean returned = new AtomicBoolean(false);

            subscription.subscribe().with(item -> {
                assertEquals(JsonValue.ValueType.NULL, item.getData().get("barSub").getValueType());
                returned.set(true);
            }, throwable -> Assertions.fail(throwable));

            await().untilTrue(returned);
        }
    }

    @Test
    public void testUnauthorizedUserForQueryWebSocket() throws Exception {
        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url)
                .header("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz")
                .executeSingleOperationsOverWebsocket(true);
        try (DynamicGraphQLClient client = clientBuilder.build()) {
            Response response = client.executeSync("{ bar { message } }");
            assertEquals(JsonValue.ValueType.NULL, response.getData().get("bar").getValueType());
        }
    }

    @Test
    public void testUnauthenticatedForQueryWebSocket() throws Exception {
        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url)
                .executeSingleOperationsOverWebsocket(true);
        try (DynamicGraphQLClient client = clientBuilder.build()) {
            Response response = client.executeSync("{ foo { message} }");
            assertEquals(JsonValue.ValueType.NULL, response.getData().get("foo").getValueType());
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

    }
}
