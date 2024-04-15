package io.quarkus.load.shedding.runtime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.load.shedding.RequestPrioritizer;
import io.quarkus.load.shedding.RequestPriority;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.vertx.core.http.HttpServerRequest;

@Singleton
public class ManagementRequestPrioritizer implements RequestPrioritizer<HttpServerRequest> {
    private final String managementPath;

    @Inject
    public ManagementRequestPrioritizer(HttpBuildTimeConfig httpConfig,
            ManagementInterfaceBuildTimeConfig managementInterfaceConfig) {
        if (managementInterfaceConfig.enabled) {
            managementPath = null;
            return;
        }
        if (httpConfig.nonApplicationRootPath.startsWith("/")) {
            if (httpConfig.nonApplicationRootPath.equals(httpConfig.rootPath)) {
                managementPath = null;
                return;
            }
            managementPath = httpConfig.nonApplicationRootPath;
            return;
        }
        managementPath = httpConfig.rootPath + httpConfig.nonApplicationRootPath;
    }

    @Override
    public boolean appliesTo(Object request) {
        if (managementPath != null && request instanceof HttpServerRequest httpRequest) {
            return httpRequest.path().startsWith(managementPath);
        }
        return false;
    }

    @Override
    public RequestPriority priority(HttpServerRequest request) {
        return RequestPriority.CRITICAL;
    }
}
