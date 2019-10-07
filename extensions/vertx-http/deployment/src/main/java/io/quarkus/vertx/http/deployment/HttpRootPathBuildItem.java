package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class HttpRootPathBuildItem extends SimpleBuildItem {

    private final String rootPath;

    public HttpRootPathBuildItem(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String adjustPath(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with /");
        }
        if (rootPath.equals("/")) {
            return path;
        }
        return rootPath + path;
    }
}
