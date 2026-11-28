package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

public class AccessTokenSharedConfigKeyNotAnnotatedTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(PropagatingGraphQLClient.class, NotAnnotatedGraphQLClient.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("GraphQL clients '" + PropagatingGraphQLClient.class.getName() + "' and '"
                            + NotAnnotatedGraphQLClient.class.getName() + "' share the configuration key 'shared-client', "
                            + "so both must have the same @AccessToken annotation or use different configuration keys"));

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @GraphQLClientApi(configKey = "shared-client")
    @AccessToken
    public interface PropagatingGraphQLClient {

        @Query
        String me();
    }

    @GraphQLClientApi(configKey = "shared-client")
    public interface NotAnnotatedGraphQLClient {

        @Query
        String me();
    }
}
