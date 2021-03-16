package io.quarkus.resteasy.reactive.client.deployment;

public interface MediaTypeWithPriority {
    int getPriority();

    String getMediaType();
}
