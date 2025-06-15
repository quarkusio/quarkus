package io.quarkus.smallrye.graphql.client.runtime;

import java.util.List;
import java.util.Map;

/**
 * Items from build time needed to make available at runtime.
 */
public class GraphQLClientSupport {

    /**
     * Allows the optional usage of short class names in GraphQL client configuration rather than fully qualified names.
     * The configuration merger bean will take it into account when merging Quarkus configuration with SmallRye-side
     * configuration.
     */
    private Map<String, String> shortNamesToQualifiedNamesMapping;

    /**
     * All config keys of clients found in the application. The reason for having this is that in TEST mode, if a config
     * key is used but doesn't have any associated configuration, we can automatically generate a configuration
     * containing a guess of the target URL.
     */
    private List<String> knownConfigKeys;

    public Map<String, String> getShortNamesToQualifiedNamesMapping() {
        return shortNamesToQualifiedNamesMapping;
    }

    public void setShortNamesToQualifiedNamesMapping(Map<String, String> shortNamesToQualifiedNamesMapping) {
        this.shortNamesToQualifiedNamesMapping = shortNamesToQualifiedNamesMapping;
    }

    public void setKnownConfigKeys(List<String> knownConfigKeys) {
        this.knownConfigKeys = knownConfigKeys;
    }

    public List<String> getKnownConfigKeys() {
        return knownConfigKeys;
    }
}
