package io.quarkus.elasticsearch.restclient.lowlevel.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Can be used for ordering the steps to execute something after the low level REST client beans are already configured.
 */
public final class ConfiguredElasticsearchLowLevelClientBuildItem extends SimpleBuildItem {

    private final Set<String> names;

    public ConfiguredElasticsearchLowLevelClientBuildItem(Set<String> names) {
        this.names = Set.copyOf(names);
    }

    /**
     * @return Names of "active" low level Elasticsearch REST clients.
     */
    public Set<String> getNames() {
        return names;
    }
}
