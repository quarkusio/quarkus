package io.quarkus.mongodb.deployment.spi;

import java.util.Collection;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Signals that the named MongoDB clients have had their schema prepared
 * (migrations applied). Consumers can declare a build-step dependency on
 * this item to ensure they initialise after the schema is ready.
 */
public final class MongoClientSchemaReadyBuildItem extends MultiBuildItem {

    private final Collection<String> clientNames;

    public MongoClientSchemaReadyBuildItem(Collection<String> clientNames) {
        this.clientNames = clientNames;
    }

    public Collection<String> getClientNames() {
        return clientNames;
    }
}
