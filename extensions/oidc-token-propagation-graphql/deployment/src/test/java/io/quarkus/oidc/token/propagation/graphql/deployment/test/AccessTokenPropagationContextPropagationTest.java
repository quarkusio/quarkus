package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.client.OidcTestClient;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(OidcWiremockTestResource.class)
public class AccessTokenPropagationContextPropagationTest {

    static final OidcTestClient client = new OidcTestClient();

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedGraphQLApi.class, PropagationTypesafeGraphQLClient.class,
                            OtherThreadFrontendResource.class))
            .withConfiguration(
                    """
                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-test}/realms/quarkus
                            quarkus.oidc.client-id=quarkus-app
                            quarkus.oidc.credentials.secret=secret

                            quarkus.smallrye-graphql-client.propagation-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            """);

    @AfterAll
    public static void close() {
        client.close();
    }

    @Test
    public void testAccessTokenIsPropagatedFromManagedExecutorThread() {
        RestAssured.given().auth().oauth2(client.getAccessToken("alice", "alice"))
                .when().get("/other-thread-frontend/managed-executor")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    public void testAccessTokenIsPropagatedFromThreadWithPropagatedContext() {
        RestAssured.given().auth().oauth2(client.getAccessToken("alice", "alice"))
                .when().get("/other-thread-frontend/propagated-context")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    public void testAccessTokenIsNotPropagatedFromThreadWithoutRequestContext() {
        RestAssured.given().auth().oauth2(client.getAccessToken("alice", "alice"))
                .when().get("/other-thread-frontend/no-context")
                .then()
                .statusCode(500)
                .body(containsString("No access token is available to propagate to the GraphQL client 'propagation-client': "
                        + "the CDI request context is not active"));
    }

    @Path("/other-thread-frontend")
    @Authenticated
    public static class OtherThreadFrontendResource {

        @Inject
        PropagationTypesafeGraphQLClient graphQLClient;

        @Inject
        ManagedExecutor managedExecutor;

        @Inject
        ThreadContext threadContext;

        private final Supplier<String> me = new Supplier<String>() {
            @Override
            public String get() {
                return graphQLClient.me();
            }
        };

        @GET
        @Path("/managed-executor")
        public String managedExecutor() throws Exception {
            return managedExecutor.supplyAsync(me).get(30, TimeUnit.SECONDS);
        }

        @GET
        @Path("/propagated-context")
        public String propagatedContext() throws Exception {
            // the CDI request context is propagated to a thread which is not managed by Quarkus
            return supplyAsync(threadContext.contextualSupplier(me));
        }

        @GET
        @Path("/no-context")
        public String noContext() throws Exception {
            return supplyAsync(me);
        }

        private static String supplyAsync(Supplier<String> supplier) throws Exception {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                return CompletableFuture.supplyAsync(supplier, executor).get(30, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
