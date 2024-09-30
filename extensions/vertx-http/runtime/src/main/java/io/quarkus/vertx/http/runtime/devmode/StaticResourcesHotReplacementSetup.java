package io.quarkus.vertx.http.runtime.devmode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.vertx.http.runtime.StaticResourcesRecorder;

public class StaticResourcesHotReplacementSetup implements HotReplacementSetup {

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        List<Path> resources = new ArrayList<>();
        for (Path classesDir : context.getClassesDir()) {
            addPathIfContainsStaticResources(resources, classesDir);
        }
        for (Path resourceDir : context.getResourcesDir()) {
            addPathIfContainsStaticResources(resources, resourceDir);
        }
        StaticResourcesRecorder.setHotDeploymentResources(resources);
    }

    @Override
    public void handleFailedInitialStart() {
    }

    @Override
    public void close() {
        StaticResourcesRecorder.setHotDeploymentResources(null);
    }

    private void addPathIfContainsStaticResources(List<Path> resources, Path resourceDir) {
        Path resource = resourceDir.resolve(StaticResourcesRecorder.META_INF_RESOURCES);
        resources.add(resource);
    }

}
