package io.quarkus.smallrye.graphql.client.runtime;

import io.smallrye.graphql.client.ErrorMessageProvider;

/**
 * Provides Quarkus-specific versions of error messages for SmallRye GraphQL. The reason being, for example,
 * the ability to suggest a quarkus.* configuration property when it's more appropriate than suggesting
 * the basic /mp-graphql property understood directly by SmallRye.
 */
public class QuarkifiedErrorMessageProvider implements ErrorMessageProvider {

    @Override
    public RuntimeException urlMissingErrorForNamedClient(String name) {
        return new RuntimeException("URL not configured for client. Please define the property " +
                "quarkus.smallrye-graphql-client." + name + ".url or pass it to your client builder dynamically");
    }

}
