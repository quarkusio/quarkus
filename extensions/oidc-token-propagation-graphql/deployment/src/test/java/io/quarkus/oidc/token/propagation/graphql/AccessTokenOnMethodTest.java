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

public class AccessTokenOnMethodTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(MethodLevelTypesafeGraphQLClient.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(DeploymentException.class)
                    .hasMessageContaining("@AccessToken on method 'me' of GraphQL client '"
                            + MethodLevelTypesafeGraphQLClient.class.getName() + "' is not supported"));

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @GraphQLClientApi(configKey = "method-level-client")
    public interface MethodLevelTypesafeGraphQLClient {

        @AccessToken
        @Query
        String me();

        @Query
        String notAnnotated();
    }
}
