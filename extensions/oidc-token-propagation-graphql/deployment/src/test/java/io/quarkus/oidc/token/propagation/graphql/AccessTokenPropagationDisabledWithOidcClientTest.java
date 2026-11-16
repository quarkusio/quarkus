package io.quarkus.oidc.token.propagation.graphql;

import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;

/**
 * The {@link io.quarkus.oidc.token.propagation.common.AccessToken} annotation is ignored when the access token
 * propagation is disabled, therefore the OIDC client integration handles the annotated GraphQL client
 * like any other GraphQL client.
 */
@QuarkusTestResource(OidcWiremockTestResource.class)
public class AccessTokenPropagationDisabledWithOidcClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            // the OIDC client integration for GraphQL clients is only added to this test
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc-client-graphql-deployment", Version.getVersion())))
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProtectedGraphQLApi.class, PropagationTypesafeGraphQLClient.class, FrontendResource.class))
            .overrideConfigKey("quarkus.oidc-token-propagation-graphql.enabled", "false")
            .withConfiguration(
                    """
                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-test}/realms/quarkus
                            quarkus.oidc.client-id=quarkus-app
                            quarkus.oidc.credentials.secret=secret

                            # the WireMock token endpoint issues a token to alice
                            quarkus.oidc-client.auth-server-url=${quarkus.oidc.auth-server-url}
                            quarkus.oidc-client.client-id=${quarkus.oidc.client-id}
                            quarkus.oidc-client.credentials.secret=${quarkus.oidc.credentials.secret}

                            quarkus.smallrye-graphql-client.propagation-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            """);

    @Test
    public void testOidcClientIsUsedInsteadOfAccessTokenPropagation() {
        RestAssured.given().auth().oauth2(OidcWiremockTestResource.getAccessToken("bob", Set.of("user")))
                .when().get("/frontend/me")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }
}
