package io.quarkus.oidc.token.propagation.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class AccessTokenAndOidcClientNameConflictTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            // the OIDC client integration for GraphQL clients is only added to this test
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc-client-graphql-deployment", Version.getVersion())))
            .withApplicationRoot((jar) -> jar.addClasses(PropagationTypesafeGraphQLClient.class))
            .overrideConfigKey("quarkus.oidc-client-graphql.propagation-client.client-name", "graphql-oidc-client")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("The 'graphql-oidc-client' OIDC client has been configured for the "
                            + "'propagation-client' GraphQL client")
                    .hasMessageContaining("'quarkus.oidc-client-graphql.\"propagation-client\".client-name'")
                    .hasMessageContaining("is already provided by the @AccessToken annotation"));

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }
}
