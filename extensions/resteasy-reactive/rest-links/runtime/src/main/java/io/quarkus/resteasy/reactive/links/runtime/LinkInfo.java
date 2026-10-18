package io.quarkus.resteasy.reactive.links.runtime;

import java.util.Set;

public final class LinkInfo {

    private final String rel;

    private final String title;

    private final String type;

    private final String entityType;

    private final String path;

    private final Set<String> pathParameters;

    public LinkInfo(String rel, String title, String type, String entityType, String path, Set<String> pathParameters) {
        this.rel = rel;
        this.title = title;
        this.type = type;
        this.entityType = entityType;
        this.path = path;
        this.pathParameters = pathParameters;
    }

    public String getRel() {
        return rel;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
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
