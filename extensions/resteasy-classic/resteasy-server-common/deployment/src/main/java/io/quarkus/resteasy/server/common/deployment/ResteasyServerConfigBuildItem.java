package io.quarkus.resteasy.server.common.deployment;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents the configuration of the RESTEasy server.
 */
public final class ResteasyServerConfigBuildItem extends SimpleBuildItem {

    private final String rootPath;

    private final String path;

    private final Map<String, String> initParameters;

    /**
     * rootPath can be different from the path if {@code @ApplicationPath} is used.
     * rootPath will not contain the {@code @ApplicationPath} while path will contain it.
     */
    public ResteasyServerConfigBuildItem(String rootPath, String path, Map<String, String> initParameters) {
        this.rootPath = rootPath;
        this.path = path.startsWith("/") ? path : "/" + path;
        this.initParameters = initParameters;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getInitParameters() {
        return initParameters;
    }
}
