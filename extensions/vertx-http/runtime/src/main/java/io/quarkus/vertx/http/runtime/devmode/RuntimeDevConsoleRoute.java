package io.quarkus.vertx.http.runtime.devmode;

import java.util.function.Function;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class RuntimeDevConsoleRoute implements Function<Router, Route> {

    private String groupId;
    private String artifactId;
    private String path;
    private String method;

    public RuntimeDevConsoleRoute(String groupId, String artifactId, String path, String method) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.path = path;
        this.method = method;
    }

    public RuntimeDevConsoleRoute() {
    }

    public String getGroupId() {
        return groupId;
    }

    public RuntimeDevConsoleRoute setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public RuntimeDevConsoleRoute setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public String getPath() {
        return path;
    }

    public RuntimeDevConsoleRoute setPath(String path) {
        this.path = path;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public RuntimeDevConsoleRoute setMethod(String method) {
        this.method = method;
        return this;
    }

    @Override
    public Route apply(Router route) {
        return route.route(HttpMethod.valueOf(method), "/@dev/" + groupId + "." + artifactId + "/" + path)
                .order(-100);
    }
}
