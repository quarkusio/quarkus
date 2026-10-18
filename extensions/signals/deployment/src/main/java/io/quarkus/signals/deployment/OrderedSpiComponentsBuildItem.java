package io.quarkus.signals.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

final class OrderedSpiComponentsBuildItem extends SimpleBuildItem {

    private final List<String> orderedEnricherIds;
    private final List<String> orderedInterceptorIds;

    OrderedSpiComponentsBuildItem(List<String> orderedEnricherIds, List<String> orderedInterceptorIds) {
        this.orderedEnricherIds = orderedEnricherIds;
        this.orderedInterceptorIds = orderedInterceptorIds;
    }

    List<String> getOrderedEnricherIds() {
        return orderedEnricherIds;
    }

    List<String> getOrderedInterceptorIds() {
        return orderedInterceptorIds;
    }

}
