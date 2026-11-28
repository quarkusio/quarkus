package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;

@QuarkusTestResource(OidcWiremockTestResource.class)
public class AccessTokenAndConfiguredAuthorizationHeaderTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(PropagationTypesafeGraphQLClient.class))
            .withConfiguration(
                    """
                            quarkus.oidc.auth-server-url=${keycloak.url:replaced-by-test}/realms/quarkus
                            quarkus.oidc.client-id=quarkus-app
                            quarkus.oidc.credentials.secret=secret
                            quarkus.smallrye-graphql-client.propagation-client.url=http://localhost:${quarkus.http.test-port:8081}/graphql
                            quarkus.smallrye-graphql-client.propagation-client.header.authorization=Basic dXNlcjpwYXNz
                            """)
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("GraphQL client 'propagation-client' is annotated with the @AccessToken annotation, "
                            + "but the 'quarkus.smallrye-graphql-client.\"propagation-client\".header.\"authorization\"' "
                            + "configuration property also sets the 'Authorization' header"));

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }
}
