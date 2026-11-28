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
import io.smallrye.graphql.client.typesafe.api.Header;

public class AccessTokenAndHeaderAnnotationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ConflictingTypesafeGraphQLClient.class, ParentGraphQLClient.class))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("GraphQL client '" + ConflictingTypesafeGraphQLClient.class.getName()
                            + "' is annotated with @AccessToken, but the @Header annotation declared in '"
                            + ParentGraphQLClient.class.getName() + "' also sets the Authorization header"));

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @GraphQLClientApi(configKey = "conflicting-client")
    @AccessToken
    public interface ConflictingTypesafeGraphQLClient extends ParentGraphQLClient {
    }

    public interface ParentGraphQLClient {

        @Query
        @Header(name = "Accept-Language", constant = "en")
        @Header(name = "authorization", constant = "Basic dXNlcjpwYXNz")
        String me();
    }
}
