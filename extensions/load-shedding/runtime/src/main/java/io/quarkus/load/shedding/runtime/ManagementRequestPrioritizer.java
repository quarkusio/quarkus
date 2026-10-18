package io.quarkus.load.shedding.runtime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.load.shedding.RequestPrioritizer;
import io.quarkus.load.shedding.RequestPriority;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class ManagementRequestPrioritizer implements RequestPrioritizer<RoutingContext> {
    private final String managementPath;

    @Inject
    public ManagementRequestPrioritizer(
            VertxHttpBuildTimeConfig buildTimeConfig,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig) {
        if (managementBuildTimeConfig.enabled()) {
            managementPath = null;
            return;
        }
        if (buildTimeConfig.nonApplicationRootPath().startsWith("/")) {
            if (buildTimeConfig.nonApplicationRootPath().equals(buildTimeConfig.rootPath())) {
                managementPath = null;
                return;
            }
            managementPath = buildTimeConfig.nonApplicationRootPath();
            return;
        }
        managementPath = buildTimeConfig.rootPath() + buildTimeConfig.nonApplicationRootPath();
    }

    @Override
    public boolean appliesTo(Object request) {
        if (managementPath != null && request instanceof RoutingContext ctx) {
            return ctx.normalizedPath().startsWith(managementPath);
        }
        return false;
    }

    @Override
    public RequestPriority priority(RoutingContext request) {
        return RequestPriority.CRITICAL;
    }
}
