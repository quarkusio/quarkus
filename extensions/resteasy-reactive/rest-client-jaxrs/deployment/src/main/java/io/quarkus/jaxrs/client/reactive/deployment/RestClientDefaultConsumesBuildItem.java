package io.quarkus.jaxrs.client.reactive.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class RestClientDefaultConsumesBuildItem extends MultiBuildItem implements MediaTypeWithPriority {
    private final String defaultMediaType;
    private final int priority;

    public RestClientDefaultConsumesBuildItem(String defaultMediaType, int priority) {
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
