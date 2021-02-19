package io.quarkus.vertx.http.deployment.devmode;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;

final public class NotFoundPageDisplayableEndpointBuildItem extends MultiBuildItem {
    private final String endpoint;
    private final String description;
    private final boolean absolutePath;

    public NotFoundPageDisplayableEndpointBuildItem(String endpoint, String description) {
        this(endpoint, description, false);
    }

    public NotFoundPageDisplayableEndpointBuildItem(String endpoint, String description, boolean absolutePath) {
        this.endpoint = endpoint;
        this.description = description;
        this.absolutePath = absolutePath;
    }

    public NotFoundPageDisplayableEndpointBuildItem(String endpoint) {
        this(endpoint, null);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAbsolutePath() {
        return absolutePath;
    }

    public String getEndpoint(HttpRootPathBuildItem httpRoot) {
        if (absolutePath) {
            return endpoint;
        } else {
            return TemplateHtmlBuilder.adjustRoot(httpRoot.getRootPath(), endpoint);
        }
    }
}
