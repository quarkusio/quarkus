package io.quarkus.jaxrs.client.reactive.deployment;

public interface MediaTypeWithPriority {
    int getPriority();

    String getMediaType();
}
