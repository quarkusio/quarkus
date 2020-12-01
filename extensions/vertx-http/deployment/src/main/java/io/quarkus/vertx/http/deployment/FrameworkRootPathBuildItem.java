package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class FrameworkRootPathBuildItem extends SimpleBuildItem {
    private final String frameworkRootPath;
    private final boolean separateRoot;

    public FrameworkRootPathBuildItem(String frameworkRootPath) {
        this.frameworkRootPath = frameworkRootPath;
        this.separateRoot = frameworkRootPath != null
                && !frameworkRootPath.equals("")
                && !frameworkRootPath.equals("/");
    }

    public String getFrameworkRootPath() {
        return frameworkRootPath;
    }

    public boolean isSeparateRoot() {
        return separateRoot;
    }

    public String adjustPath(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with /");
        }
        if (frameworkRootPath.equals("/")) {
            return path;
        }
        return frameworkRootPath + path;
    }
}
