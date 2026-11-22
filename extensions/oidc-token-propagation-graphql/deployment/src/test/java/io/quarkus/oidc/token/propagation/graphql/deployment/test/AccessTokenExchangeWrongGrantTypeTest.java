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
public class AccessTokenExchangeWrongGrantTypeTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(WrongGrantTypeGraphQLClient.class))
            .withConfiguration(
                    """
                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-test}/realms/quarkus
                            quarkus.oidc.client-id=quarkus-app
                            quarkus.oidc.credentials.secret=secret

                            # the default OIDC client uses the client credentials grant
                            quarkus.oidc-client.auth-server-url=${quarkus.oidc.auth-server-url}
                            quarkus.oidc-client.client-id=${quarkus.oidc.client-id}
                            quarkus.oidc-client.credentials.secret=${quarkus.oidc.credentials.secret}

                            quarkus.smallrye-graphql-client.wrong-grant-type.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            """)
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("GraphQL client 'wrong-grant-type' requires the default OIDC client to exchange "
                            + "the access token, but this OIDC client is configured to use the client_credentials grant type"));

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @GraphQLClientApi(configKey = "wrong-grant-type")
    @AccessToken(exchangeTokenClient = "Default")
    public interface WrongGrantTypeGraphQLClient {

        @Query
        String me();
    }
}
