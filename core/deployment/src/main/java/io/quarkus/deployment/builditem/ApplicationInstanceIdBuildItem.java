package io.quarkus.deployment.builditem;

import java.util.UUID;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A unique identifier for an instance of an application.
 * Development and test modes will have different IDs.
 * The application ID will persist across continuous test restarts and dev mode restarts.
 * It mirrors the lifecycle of a curated application.
 *
 */
public final class ApplicationInstanceIdBuildItem extends SimpleBuildItem {

    final UUID UUID;

    public ApplicationInstanceIdBuildItem(UUID uuid) {
        this.UUID = uuid;
    }

    public UUID getUUID() {
        return UUID;
    }
}
