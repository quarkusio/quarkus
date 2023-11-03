package io.quarkus.vertx.http.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Meant to be used by extensions that generate resource into {@code META-INF/resources}.
 * These resources cannot be picked up automatically by the standard Static resources handling because
 * when the check is made, these resources don't exist yet on the file system.
 *
 * The value of {@code path} should be prefixed with {@code '/'} and is assumed to be a path under {@code 'META-INF/resources'}.
 */
public final class AdditionalStaticResourceBuildItem extends MultiBuildItem {

    private final String path;
    private final boolean isDirectory;

    public AdditionalStaticResourceBuildItem(String path, boolean isDirectory) {
        this.path = path;
        this.isDirectory = isDirectory;
    }

    public String getPath() {
        return path;
    }

    public boolean isDirectory() {
        return isDirectory;
    }
}
