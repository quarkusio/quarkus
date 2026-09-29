package io.quarkus.resteasy.reactive.server.deployment;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRuntimeRecorder;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem;
import io.quarkus.vertx.http.deployment.spi.StaticResourcesBuildItem;

class StaticResourceShadowingProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void checkStaticResourceShadowing(ResteasyReactiveRuntimeRecorder recorder,
            Capabilities capabilities,
            LaunchModeBuildItem launchMode,
            Optional<ResteasyReactiveDeploymentBuildItem> deployment,
            Optional<StaticResourcesBuildItem> staticResources,
            List<GeneratedStaticResourceBuildItem> generatedStaticResources,
            HttpRootPathBuildItem httpRootPath) {
        // with the servlet container, Quarkus REST is served before the static resources
        if (deployment.isEmpty() || capabilities.isPresent(Capability.SERVLET)) {
            return;
        }
        Set<String> staticFilePaths = new TreeSet<>();
        staticResources.ifPresent(resources -> staticFilePaths.addAll(resources.getPaths()));
        if (!launchMode.getLaunchMode().isProduction()) {
            // outside production, the generated static resources are served by their own route instead of being part
            // of the static resources, see GeneratedStaticResourcesProcessor
            for (GeneratedStaticResourceBuildItem resource : generatedStaticResources) {
                staticFilePaths.add(resource.getEndpoint());
            }
        }
        if (!staticFilePaths.isEmpty()) {
            recorder.checkStaticResourceShadowing(deployment.get().getDeployment(), httpRootPath.getRootPath(),
                    staticFilePaths);
        }
    }
}
