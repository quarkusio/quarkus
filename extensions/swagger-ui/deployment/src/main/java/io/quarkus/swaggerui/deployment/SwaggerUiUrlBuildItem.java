package io.quarkus.swaggerui.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that allows extensions to register custom URLs for the Swagger UI.
 */
public final class SwaggerUiUrlBuildItem extends MultiBuildItem {
    private final String name;
    private final String url;

    public SwaggerUiUrlBuildItem(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
