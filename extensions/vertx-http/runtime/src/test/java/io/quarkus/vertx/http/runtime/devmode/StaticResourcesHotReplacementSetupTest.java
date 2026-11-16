package io.quarkus.vertx.http.runtime.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.vertx.http.runtime.StaticResourcesRecorder;

public class StaticResourcesHotReplacementSetupTest {

    private static final Path DEP_CLASSES = Path.of("dep", "target", "classes");
    private static final Path APP_CLASSES = Path.of("app", "target", "classes");
    private static final Path DEP_RESOURCES = Path.of("dep", "src", "main", "resources");
    private static final Path APP_RESOURCES = Path.of("app", "src", "main", "resources");

    @Test
    public void applicationModuleIsConsultedBeforeDependencyModules() {
        List<Path> roots = StaticResourcesHotReplacementSetup.hotDeploymentRoots(
                List.of(DEP_CLASSES, APP_CLASSES),
                List.of(APP_RESOURCES, DEP_RESOURCES));

        assertThat(roots).containsExactly(
                metaInfResources(APP_CLASSES),
                metaInfResources(DEP_CLASSES),
                metaInfResources(APP_RESOURCES),
                metaInfResources(DEP_RESOURCES));
    }

    @Test
    public void singleModuleKeepsClassesBeforeResources() {
        List<Path> roots = StaticResourcesHotReplacementSetup.hotDeploymentRoots(
                List.of(APP_CLASSES),
                List.of(APP_RESOURCES));

        assertThat(roots).containsExactly(
                metaInfResources(APP_CLASSES),
                metaInfResources(APP_RESOURCES));
    }

    private static Path metaInfResources(Path root) {
        return root.resolve(StaticResourcesRecorder.META_INF_RESOURCES);
    }
}
