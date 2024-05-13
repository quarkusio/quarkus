package io.quarkus.vertx.http.deployment.devmode;

import java.util.Arrays;
import java.util.stream.Collectors;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.devmode.RouteDescription;
import io.quarkus.vertx.http.runtime.devmode.RouteMethodDescription;

public final class RouteDescriptionBuildItem extends MultiBuildItem {

    private RouteDescription description;

    public RouteDescriptionBuildItem(String javaMethod, String path, String httpMethod, String[] produces, String[] consumes) {
        RouteDescription description = new RouteDescription();

        description.setBasePath(path);
        description.addCall(new RouteMethodDescription(javaMethod,
                httpMethod,
                path,
                getMediaType(produces),
                getMediaType(consumes)));

        this.description = description;
    }

    public RouteDescription getDescription() {
        return description;
    }

    private String getMediaType(String[] all) {
        return all.length == 0 ? null : Arrays.stream(all).collect(Collectors.joining(", "));
    }

}
