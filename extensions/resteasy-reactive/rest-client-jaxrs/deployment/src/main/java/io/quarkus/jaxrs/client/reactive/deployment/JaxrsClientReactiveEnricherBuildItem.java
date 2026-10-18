package io.quarkus.jaxrs.client.reactive.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * BuildItem to register a {@link JaxrsClientReactiveEnricher}
 */
public final class JaxrsClientReactiveEnricherBuildItem extends MultiBuildItem {
    private final JaxrsClientReactiveEnricher enricher;

    public JaxrsClientReactiveEnricherBuildItem(JaxrsClientReactiveEnricher enricher) {
        this.enricher = enricher;
    }

    public JaxrsClientReactiveEnricher getEnricher() {
        return enricher;
    }
}
