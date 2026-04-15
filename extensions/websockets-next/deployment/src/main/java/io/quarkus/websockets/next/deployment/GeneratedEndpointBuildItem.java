package io.quarkus.websockets.next.deployment;

import java.util.Comparator;

import org.jspecify.annotations.NonNull;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A generated representation of a WebSocket endpoint.
 */
public final class GeneratedEndpointBuildItem extends MultiBuildItem implements Comparable<GeneratedEndpointBuildItem> {

    public final String endpointId;
    public final String endpointClassName;
    public final String generatedClassName;
    public final String path;
    public final boolean isClient;

    GeneratedEndpointBuildItem(String endpointId, String endpointClassName, String generatedClassName, String path,
            boolean isClient) {
        this.endpointId = endpointId;
        this.endpointClassName = endpointClassName;
        this.generatedClassName = generatedClassName;
        this.path = path;
        this.isClient = isClient;
    }

    public boolean isServer() {
        return !isClient;
    }

    public boolean isClient() {
        return isClient;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public String getEndpointClassName() {
        return endpointClassName;
    }

    public String getGeneratedClassName() {
        return generatedClassName;
    }

    public String getPath() {
        return path;
    }

    private static final Comparator<GeneratedEndpointBuildItem> COMPARATOR = Comparator
            .comparing(GeneratedEndpointBuildItem::getEndpointId, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(GeneratedEndpointBuildItem::getEndpointClassName, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(GeneratedEndpointBuildItem::getGeneratedClassName, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(GeneratedEndpointBuildItem::getPath, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(GeneratedEndpointBuildItem::isClient);

    @Override
    public int compareTo(@NonNull GeneratedEndpointBuildItem o) {
        return COMPARATOR.compare(this, o);
    }
}
