package io.quarkus.smallrye.graphql.client.runtime;

import java.util.Map;

/**
 * Items from build time needed to make available at runtime.
 */
public class GraphQLClientSupport {

    /**
     * Allows the optional usage of short class names in GraphQL client configuration rather than
     * fully qualified names. The configuration merger bean will take it into account
     * when merging Quarkus configuration with SmallRye-side configuration.
     */
    private Map<String, String> shortNamesToQualifiedNamesMapping;

    public Map<String, String> getShortNamesToQualifiedNamesMapping() {
        return shortNamesToQualifiedNamesMapping;
    }

    public void setShortNamesToQualifiedNamesMapping(Map<String, String> shortNamesToQualifiedNamesMapping) {
        this.shortNamesToQualifiedNamesMapping = shortNamesToQualifiedNamesMapping;
    }
}
