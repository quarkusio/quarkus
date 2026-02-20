package io.quarkus.elasticsearch.javaclient.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a reference (direct or indirect) to an Elasticsearch Java client detected at compile time.
 * <p>
 * Used in particular to determine which clients should have associated beans.
 */
public final class ElasticsearchJavaClientReferenceBuildItem extends MultiBuildItem {

    private final String name;

    public ElasticsearchJavaClientReferenceBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
