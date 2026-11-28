package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.graphql.client.typesafe.api.AuthorizationHeader;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

public class AccessTokenAndAuthorizationHeaderAnnotationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ConflictingTypesafeGraphQLClient.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("GraphQL client '" + ConflictingTypesafeGraphQLClient.class.getName()
                            + "' is annotated with @AccessToken, but the @AuthorizationHeader annotation declared in '"
                            + ConflictingTypesafeGraphQLClient.class.getName() + "' also sets the Authorization header"));

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @GraphQLClientApi(configKey = "conflicting-client")
    @AccessToken
    public interface ConflictingTypesafeGraphQLClient {

        @Query
        @AuthorizationHeader
        String me();
    }
}
