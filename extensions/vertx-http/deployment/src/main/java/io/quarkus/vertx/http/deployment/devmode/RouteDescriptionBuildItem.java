package io.quarkus.vertx.http.deployment.devmode;

import java.util.Arrays;
import java.util.stream.Collectors;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.devmode.RouteDescription;

public final class RouteDescriptionBuildItem extends MultiBuildItem {

    private RouteDescription description;

    public RouteDescriptionBuildItem(String javaMethod, String path, String httpMethod, String[] produces, String[] consumes) {
        RouteDescription description = new RouteDescription();
        description.setJavaMethod(javaMethod);
        description.setPath(path);
        description.setHttpMethod(httpMethod);
        description.setProduces(produces.length == 0 ? null : Arrays.stream(produces).collect(Collectors.joining(", ")));
        description.setConsumes(consumes.length == 0 ? null : Arrays.stream(consumes).collect(Collectors.joining(", ")));
        this.description = description;
    }

    public RouteDescription getDescription() {
        return description;
    }

}
