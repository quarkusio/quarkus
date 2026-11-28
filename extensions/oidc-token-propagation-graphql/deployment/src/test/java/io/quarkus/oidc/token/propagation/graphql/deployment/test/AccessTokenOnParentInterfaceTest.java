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

public class AccessTokenOnParentInterfaceTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ParentInterface.class, ChildTypesafeGraphQLClient.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("@AccessToken on the parent interface '" + ParentInterface.class.getName()
                            + "' of GraphQL client '" + ChildTypesafeGraphQLClient.class.getName() + "' is not supported"));

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @AccessToken
    public interface ParentInterface {

        @Query
        String me();
    }

    @GraphQLClientApi(configKey = "child-client")
    public interface ChildTypesafeGraphQLClient extends ParentInterface {
    }
}
