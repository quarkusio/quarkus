package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Stream;

import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonValue;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.graphql.api.Subscription;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.UnexpectedCloseException;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;
import io.smallrye.graphql.client.websocket.WebsocketSubprotocol;
import io.smallrye.mutiny.Multi;

/**
 * Due to the complexity of establishing a WebSocket, WebSocket/Subscription testing of the GraphQL server is done here,
 * as the client framework comes in very useful for establishing the connection to the server.
 * <br>
 * This test ensures that websockets that are established with Authorization inside the connection_init payload are
 * authenticated and authorized in the same way that if included in the header.
 * The only difference being that the endpoint cannot be secured with HTTP permissions, as the connection_init payload
 * is only sent after the websocket is opened.
 */
public class DynamicGraphQLClientWebSocketAuthenticationClientInitTest {

    static String url = "http://" + System.getProperty("quarkus.http.host", "localhost") + ":" +
            System.getProperty("quarkus.http.test-port", "8081") + "/graphql";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredApi.class, Foo.class)
                    .addAsResource("application-secured.properties", "application.properties")
                    .addAsResource("users.properties")
                    .addAsResource("roles.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.smallrye-graphql.authorization-client-init-payload-name", "Authorization");

    private static Stream<Arguments> websocketArguments() {
        return Stream.of(WebsocketSubprotocol.values())
                .map(Enum::name)
                .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("websocketArguments")
    public void testAuthenticatedUserForQueryWebSocketOverInitParams(String subprotocolName) throws Exception {
        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url)
                .initPayload(Map.of("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz"))
                .executeSingleOperationsOverWebsocket(true)
                .subprotocols(WebsocketSubprotocol.valueOf(subprotocolName));
        try (DynamicGraphQLClient client = clientBuilder.build()) {
            // Test that repeated queries yields the same result
            for (int i = 0; i < 3; i++) {
                Response response = client.executeSync("{ foo { message} }");
                assertTrue(response.hasData());
                assertEquals("foo", response.getData().getJsonObject("foo").getString("message"));
            }

            // Unauthorized query
            Response response = client.executeSync("{ bar { message} }");
            assertTrue(response.hasData());
            assertEquals(JsonValue.ValueType.NULL, response.getData().get("bar").getValueType());
        }
    }

    @ParameterizedTest
    @MethodSource("websocketArguments")
    public void testUnauthenticatedUserForQueryWebSocketOverInitParams(String subprotocolName) throws Exception {
        // Validate that our unit test code actually has a correctly secured endpoint
        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url)
                .executeSingleOperationsOverWebsocket(true)
                .subprotocols(WebsocketSubprotocol.valueOf(subprotocolName));
        try (DynamicGraphQLClient client = clientBuilder.build()) {
            Response response = client.executeSync("{ foo { message} }");
            assertTrue(response.hasData());
            assertEquals(JsonValue.ValueType.NULL, response.getData().get("foo").getValueType());
        }
    }

    @ParameterizedTest
    @MethodSource("websocketArguments")
    @Disabled("Reliant on next version of SmallRye GraphQL to prevent it hanging")
    public void testIncorrectCredentialsForQueryWebSocketOverInitParams(String subprotocolName) {
        UnexpectedCloseException exception = assertThrows(UnexpectedCloseException.class, () -> {
            // Validate that our unit test code actually has a correctly secured endpoint
            DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                    .url(url)
                    .executeSingleOperationsOverWebsocket(true)
                    .initPayload(Map.of("Authorization", "Basic ZnJlZDpXUk9OR19QQVNTV09SRA=="))
                    .subprotocols(WebsocketSubprotocol.valueOf(subprotocolName));
            try (DynamicGraphQLClient client = clientBuilder.build()) {
                client.executeSync("{ foo { message} }");
            }
        });
        assertEquals((short) 4403, exception.getCloseStatusCode());
        assertTrue(exception.getMessage().contains("Forbidden"));
    }

    @ParameterizedTest
    @MethodSource("websocketArguments")
    @Disabled("Reliant on next version of SmallRye GraphQL to prevent it hanging")
    public void testAuthenticatedUserForQueryWebSocketOverHeadersAndInitParams(String subprotocolName) {
        UnexpectedCloseException exception = assertThrows(UnexpectedCloseException.class, () -> {
            DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                    .url(url)
                    // Header takes precedence over init payload
                    .header("Authorization", "Basic ZnJlZDpmb28=")
                    // This should be ignored as the header is set
                    .initPayload(Map.of("Authorization", "Basic ZGF2aWQ6cXdlcnR5MTIz"))
                    .executeSingleOperationsOverWebsocket(true)
                    .subprotocols(WebsocketSubprotocol.valueOf(subprotocolName));
            try (DynamicGraphQLClient client = clientBuilder.build()) {
                // Executing the query should fail because the server will error as we've defined two methods of auth
                client.executeSync("{ foo { message} }");
            }
        });
        assertEquals((short) 4400, exception.getCloseStatusCode());
        assertTrue(exception.getMessage().contains("Authorization specified in multiple locations"));
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
