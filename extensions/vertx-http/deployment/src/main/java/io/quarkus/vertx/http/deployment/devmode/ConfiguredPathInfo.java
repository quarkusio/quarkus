package io.quarkus.vertx.http.deployment.devmode;

import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.TemplateHtmlBuilder;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

public class ConfiguredPathInfo {
    private final String name;
    private final String endpointPath;
    private final boolean absolutePath;
    private final boolean management;

    public ConfiguredPathInfo(String name, String endpointPath, boolean isAbsolutePath, boolean management) {
        this.name = name;
        this.endpointPath = endpointPath;
        this.absolutePath = isAbsolutePath;
        this.management = management;
    }

    public String getName() {
        return name;
    }

    public String getEndpointPath(HttpRootPathBuildItem httpRoot) {
        if (absolutePath) {
            return endpointPath;
        } else {
            return TemplateHtmlBuilder.adjustRoot(httpRoot.getRootPath(), endpointPath);
        }
    }

    public String getEndpointPath(NonApplicationRootPathBuildItem nonAppRoot,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            LaunchModeBuildItem mode) {
        if (absolutePath) {
            return endpointPath;
        }
        if (management && managementBuildTimeConfig.enabled()) {
            var prefix = NonApplicationRootPathBuildItem.getManagementUrlPrefix(mode);
            return prefix + endpointPath;
        } else {
            return TemplateHtmlBuilder.adjustRoot(nonAppRoot.getNormalizedHttpRootPath(), endpointPath);
        }
    }
}
