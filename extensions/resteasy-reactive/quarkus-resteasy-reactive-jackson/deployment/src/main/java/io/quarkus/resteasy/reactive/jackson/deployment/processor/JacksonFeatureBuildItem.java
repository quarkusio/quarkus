package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Contains the special Jackson features required by the application
 */
public final class JacksonFeatureBuildItem extends MultiBuildItem {

    private final Feature feature;

    public JacksonFeatureBuildItem(Feature feature) {
        this.feature = feature;
    }

    public Feature getFeature() {
        return feature;
    }

    public enum Feature {
        JSON_VIEW,
        CUSTOM_SERIALIZATION
    }
}
