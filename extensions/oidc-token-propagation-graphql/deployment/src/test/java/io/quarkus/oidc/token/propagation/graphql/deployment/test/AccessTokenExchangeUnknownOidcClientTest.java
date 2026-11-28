package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

@QuarkusTestResource(OidcWiremockTestResource.class)
public class AccessTokenExchangeUnknownOidcClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(UnknownOidcClientGraphQLClient.class))
            .withConfiguration(
                    """
                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-test}/realms/quarkus
                            quarkus.oidc.client-id=quarkus-app
                            quarkus.oidc.credentials.secret=secret
                            quarkus.smallrye-graphql-client.unknown-oidc-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            """)
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("GraphQL client 'unknown-oidc-client' requires the 'unknown' OIDC client to exchange "
                            + "the access token, but there is no such OIDC client"));

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @GraphQLClientApi(configKey = "unknown-oidc-client")
    @AccessToken(exchangeTokenClient = "unknown")
    public interface UnknownOidcClientGraphQLClient {

        @Query
        String me();
    }
}
