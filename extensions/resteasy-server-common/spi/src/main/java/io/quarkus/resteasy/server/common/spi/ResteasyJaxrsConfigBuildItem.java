package io.quarkus.resteasy.server.common.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents a JAX-RS config.
 */
public final class ResteasyJaxrsConfigBuildItem extends SimpleBuildItem {

    private final String rootPath;

    private final String defaultPath;

    /**
     * rootPath can be different from the defaultPath if {@code @ApplicationPath} is used.
     * rootPath will not contain the {@code @ApplicationPath} while defaultPath will contain it.
     */
    public ResteasyJaxrsConfigBuildItem(String rootPath, String defaultPath) {
        this.rootPath = rootPath;
        this.defaultPath = defaultPath;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getDefaultPath() {
        return defaultPath;
    }
}
