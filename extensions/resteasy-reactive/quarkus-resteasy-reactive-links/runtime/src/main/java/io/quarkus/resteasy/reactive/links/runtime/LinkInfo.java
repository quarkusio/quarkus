package io.quarkus.resteasy.reactive.links.runtime;

import java.util.Set;

import io.quarkus.runtime.annotations.RecordableConstructor;

public final class LinkInfo {

    private final String rel;

    private final String entityType;

    private final String path;

    private final Set<String> pathParameters;

    @RecordableConstructor
    public LinkInfo(String rel, String entityType, String path, Set<String> pathParameters) {
        this.rel = rel;
        this.entityType = entityType;
        this.path = path;
        this.pathParameters = pathParameters;
    }

    public String getRel() {
        return rel;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getPath() {
        return path;
    }

    public Set<String> getPathParameters() {
        return pathParameters;
    }
}
