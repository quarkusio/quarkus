package io.quarkus.swaggerui.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

final class SwaggerUiBuildItem extends SimpleBuildItem {

    private final String swaggerUiFinalDestination;
    private final String swaggerUiPath;

    public SwaggerUiBuildItem(String swaggerUiFinalDestination, String swaggerUiPath) {
        this.swaggerUiFinalDestination = swaggerUiFinalDestination;
        this.swaggerUiPath = swaggerUiPath;
    }

    public String getSwaggerUiFinalDestination() {
        return swaggerUiFinalDestination;
    }

    public String getSwaggerUiPath() {
        return swaggerUiPath;
    }
}