package io.quarkus.oidc.token.propagation.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

public class AccessTokenSharedConfigKeyConflictTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(PropagatingGraphQLClient.class, ExchangingGraphQLClient.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(DeploymentException.class)
                    .hasMessageContaining(PropagatingGraphQLClient.class.getName())
                    .hasMessageContaining(ExchangingGraphQLClient.class.getName())
                    .hasMessageContaining("share the configuration key 'shared-client'"));

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
    @AccessToken(exchangeTokenClient = "Default")
    public interface ExchangingGraphQLClient {

        @Query
        String me();
    }
}
