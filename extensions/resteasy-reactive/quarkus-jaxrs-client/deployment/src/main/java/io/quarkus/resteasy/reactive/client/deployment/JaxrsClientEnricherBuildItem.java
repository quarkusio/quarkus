package io.quarkus.resteasy.reactive.client.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * BuildItem to register a {@link JaxrsClientEnricher}
 */
public final class JaxrsClientEnricherBuildItem extends MultiBuildItem {
    private final JaxrsClientEnricher enricher;

    public JaxrsClientEnricherBuildItem(JaxrsClientEnricher enricher) {
        this.enricher = enricher;
    }

    public JaxrsClientEnricher getEnricher() {
        return enricher;
    }
}
