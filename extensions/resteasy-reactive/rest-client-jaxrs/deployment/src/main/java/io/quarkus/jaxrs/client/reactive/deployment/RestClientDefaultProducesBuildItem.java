package io.quarkus.jaxrs.client.reactive.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class RestClientDefaultProducesBuildItem extends MultiBuildItem implements MediaTypeWithPriority {
    private final String defaultMediaType;
    private final int priority;

    public RestClientDefaultProducesBuildItem(String defaultMediaType, int priority) {
        this.defaultMediaType = defaultMediaType;
        this.priority = priority;
    }

    @Override
    public String getMediaType() {
        return defaultMediaType;
    }

    @Override
    public int getPriority() {
        return priority;
    }
}
