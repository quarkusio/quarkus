package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;

/**
 * The application cannot start if there is no {@link io.quarkus.security.credential.TokenCredential} bean
 * whose token the GraphQL clients could propagate.
 */
@QuarkusTestResource(OidcWiremockTestResource.class)
public class AccessTokenWithoutTokenCredentialTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(PropagationTypesafeGraphQLClient.class))
            // the OIDC extension is the only one which provides the TokenCredential bean in this module
            .overrideConfigKey("quarkus.arc.exclude-types", "io.quarkus.oidc.runtime.OidcTokenCredentialProducer")
            .withConfiguration(
                    """
                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-test}/realms/quarkus
                            quarkus.oidc.client-id=quarkus-app
                            quarkus.oidc.credentials.secret=secret
                            quarkus.smallrye-graphql-client.propagation-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            """)
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("GraphQL clients annotated with the @AccessToken annotation propagate the token "
                            + "of the TokenCredential bean, but there is no such bean")
                    .hasMessageContaining("quarkus-oidc or quarkus-smallrye-jwt"));

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }
}
