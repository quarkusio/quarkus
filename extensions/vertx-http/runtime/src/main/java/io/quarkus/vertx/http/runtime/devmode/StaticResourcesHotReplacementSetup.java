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
        StaticResourcesRecorder.setHotDeploymentResources(
                hotDeploymentRoots(context.getClassesDir(), context.getResourcesDir()));
    }

    /**
     * Computes the static resource roots served in dev mode, in lookup order. The first root that contains a
     * requested file wins, so the application module's roots must come before those of its workspace dependencies.
     *
     * @param classesDirs the output directories of all workspace modules, application module last, as returned by
     *        {@link HotReplacementContext#getClassesDir()}
     * @param resourcesDirs the resource directories of all workspace modules, application module first, as returned by
     *        {@link HotReplacementContext#getResourcesDir()}
     * @return the {@code META-INF/resources} directories to serve, in lookup order
     */
    static List<Path> hotDeploymentRoots(List<Path> classesDirs, List<Path> resourcesDirs) {
        List<Path> resources = new ArrayList<>();
        for (int i = classesDirs.size() - 1; i >= 0; i--) {
            addPathIfContainsStaticResources(resources, classesDirs.get(i));
        }
        for (Path resourceDir : resourcesDirs) {
            addPathIfContainsStaticResources(resources, resourceDir);
        }
        return resources;
    }

    @Override
    public void handleFailedInitialStart() {
    }

    @Override
    public void close() {
        StaticResourcesRecorder.setHotDeploymentResources(null);
    }

    private static void addPathIfContainsStaticResources(List<Path> resources, Path resourceDir) {
        Path resource = resourceDir.resolve(StaticResourcesRecorder.META_INF_RESOURCES);
        resources.add(resource);
    }

}
