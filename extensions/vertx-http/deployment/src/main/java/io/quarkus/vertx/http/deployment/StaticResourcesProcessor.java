package io.quarkus.vertx.http.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.paths.PathVisitor;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.spi.AdditionalStaticResourceBuildItem;
import io.quarkus.vertx.http.deployment.spi.StaticResourcesBuildItem;
import io.quarkus.vertx.http.runtime.StaticResourcesRecorder;

/**
 * Handles all static file resources found in {@code META-INF/resources} unless the servlet container is present.
 */
public class StaticResourcesProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    HotDeploymentWatchedFileBuildItem indexHtmlFile() {
        String staticRoot = StaticResourcesRecorder.META_INF_RESOURCES + "/index.html";
        return new HotDeploymentWatchedFileBuildItem(staticRoot, !QuarkusClassLoader.isResourcePresentAtRuntime(staticRoot));
    }

    @BuildStep
    void collectStaticResources(Capabilities capabilities,
            List<AdditionalStaticResourceBuildItem> additionalStaticResources,
            BuildProducer<StaticResourcesBuildItem> staticResources,
            LaunchModeBuildItem launchModeBuildItem) {
        if (capabilities.isPresent(Capability.SERVLET)) {
            // Servlet container handles static resources
            return;
        }
        Set<StaticResourcesBuildItem.Entry> paths = getClasspathResources();
        // We shouldn't add them in test and dev-mode (as they are handled by the GeneratedStaticResourcesProcessor), but for backward compatibility we keep it for now
        for (AdditionalStaticResourceBuildItem bi : additionalStaticResources) {
            paths.add(new StaticResourcesBuildItem.Entry(bi.getPath(), bi.isDirectory()));
        }

        if (!paths.isEmpty()) {
            staticResources.produce(new StaticResourcesBuildItem(paths));
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void runtimeInit(Optional<StaticResourcesBuildItem> staticResources, StaticResourcesRecorder recorder,
            CoreVertxBuildItem vertx, BeanContainerBuildItem beanContainer,
            BuildProducer<DefaultRouteBuildItem> defaultRoutes) {
        if (staticResources.isPresent()) {
            defaultRoutes.produce(new DefaultRouteBuildItem(recorder.start(staticResources.get().getPaths())));
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void nativeImageResource(Optional<StaticResourcesBuildItem> staticResources,
            BuildProducer<NativeImageResourceBuildItem> producer) {
        if (staticResources.isPresent()) {
            Set<StaticResourcesBuildItem.Entry> entries = staticResources.get().getEntries();
            List<String> metaInfResources = new ArrayList<>(entries.size());
            for (StaticResourcesBuildItem.Entry entry : entries) {
                if (entry.isDirectory()) {
                    // TODO: do we perhaps want to register the whole directory?
                    continue;
                }

                String metaInfResourcesPath = StaticResourcesRecorder.META_INF_RESOURCES + entry.getPath();
                metaInfResources.add(metaInfResourcesPath);
            }
            producer.produce(new NativeImageResourceBuildItem(metaInfResources));

            // register all directories under META-INF/resources for reflection in order to enable
            // the serving of index.html in arbitrarily nested directories
            final Set<String> collectedDirs = new HashSet<>();
            visitRuntimeMetaInfResources(visit -> {
                if (Files.isDirectory(visit.getPath())) {
                    final String relativePath = visit.getRelativePath();
                    if (collectedDirs.add(relativePath)) {
                        producer.produce(new NativeImageResourceBuildItem(relativePath));
                    }
                }
            });
        }
    }

    /**
     * Find all static file resources that are available from classpath.
     *
     * @return the set of static resources
     */
    private Set<StaticResourcesBuildItem.Entry> getClasspathResources() {
        Set<StaticResourcesBuildItem.Entry> knownPaths = new HashSet<>();
        visitRuntimeMetaInfResources(visit -> {
            if (!Files.isDirectory(visit.getPath())) {
                knownPaths.add(new StaticResourcesBuildItem.Entry(
                        visit.getRelativePath().substring(StaticResourcesRecorder.META_INF_RESOURCES.length()),
                        false));
            }
        });
        return knownPaths;
    }

    /**
     * Visits all {@code META-INF/resources} directories and their content found on the runtime classpath
     *
     * @param visitor visitor implementation
     */
    private static void visitRuntimeMetaInfResources(PathVisitor visitor) {
        final List<ClassPathElement> elements = QuarkusClassLoader.getElements(StaticResourcesRecorder.META_INF_RESOURCES,
                false);
        if (!elements.isEmpty()) {
            for (var element : elements) {
                if (element.isRuntime()) {
                    element.apply(tree -> {
                        tree.walkIfContains(StaticResourcesRecorder.META_INF_RESOURCES, visitor);
                        return null;
                    });
                }
            }
        }
    }
}
