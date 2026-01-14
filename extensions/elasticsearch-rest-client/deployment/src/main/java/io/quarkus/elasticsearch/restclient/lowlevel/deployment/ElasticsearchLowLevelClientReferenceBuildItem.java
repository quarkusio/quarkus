package io.quarkus.elasticsearch.restclient.lowlevel.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a reference (direct or indirect) to an Elasticsearch low-level client detected at compile time.
 * <p>
 * Used in particular to determine which clients should have associated beans.
 */
public final class ElasticsearchLowLevelClientReferenceBuildItem extends MultiBuildItem {

    private final String name;

    public ElasticsearchLowLevelClientReferenceBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
