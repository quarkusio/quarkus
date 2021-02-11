package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class NonApplicationRootPathBuildItem extends SimpleBuildItem {
    private final String httpRootPath;
    private final String frameworkRootPath;
    private final boolean separateRoot;

    public NonApplicationRootPathBuildItem(String frameworkRootPath) {
        this(frameworkRootPath, null);
    }

    public NonApplicationRootPathBuildItem(String frameworkRootPath, String httpRootPath) {
        this.frameworkRootPath = frameworkRootPath;
        this.separateRoot = frameworkRootPath != null
                && !frameworkRootPath.equals("")
                && !frameworkRootPath.equals("/");
        this.httpRootPath = httpRootPath;
    }

    public String getFrameworkRootPath() {
        return frameworkRootPath;
    }

    public boolean isSeparateRoot() {
        return separateRoot;
    }

    /**
     * Adjusts a path by including the non-application root path.
     */
    public String adjustPath(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with /");
        }
        if (frameworkRootPath.equals("/")) {
            return path;
        }
        return frameworkRootPath + path;
    }

    /**
     * Adjusts a path by including both the non-application root path and
     * the HTTP root path.
     */
    public String adjustPathIncludingHttpRootPath(String path) {
        String withFrameWorkPath = adjustPath(path);
        if (httpRootPath == null || httpRootPath.equals("/")) {
            return withFrameWorkPath;
        }
        return httpRootPath + withFrameWorkPath;
    }
}
