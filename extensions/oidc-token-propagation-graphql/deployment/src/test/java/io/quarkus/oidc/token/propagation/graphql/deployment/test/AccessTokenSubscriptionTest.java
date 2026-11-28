package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.graphql.api.Subscription;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.mutiny.Multi;

public class AccessTokenSubscriptionTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(SubscriptionTypesafeGraphQLClient.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("GraphQL client '" + SubscriptionTypesafeGraphQLClient.class.getName()
                            + "' is annotated with @AccessToken, but its method 'names' is a subscription"));

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @GraphQLClientApi(configKey = "subscription-client")
    @AccessToken
    public interface SubscriptionTypesafeGraphQLClient {

        @Query
        String me();

        @Subscription
        Multi<String> names();
    }
}
