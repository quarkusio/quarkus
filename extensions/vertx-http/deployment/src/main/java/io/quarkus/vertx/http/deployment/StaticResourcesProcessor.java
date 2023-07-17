package io.quarkus.vertx.http.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.spi.AdditionalStaticResourceBuildItem;
import io.quarkus.vertx.http.deployment.spi.StaticResourcesBuildItem;
import io.quarkus.vertx.http.runtime.StaticResourcesRecorder;

/**
 * Handles all static file resources found in {@code META-INF/resources} unless the servlet container is present.
 */
public class StaticResourcesProcessor {

    @BuildStep
    void collectStaticResources(Capabilities capabilities, ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<AdditionalStaticResourceBuildItem> additionalStaticResources,
            BuildProducer<StaticResourcesBuildItem> staticResources) throws Exception {
        if (capabilities.isPresent(Capability.SERVLET)) {
            // Servlet container handles static resources
            return;
        }
        Set<StaticResourcesBuildItem.Entry> paths = getClasspathResources(applicationArchivesBuildItem);
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
        }
    }

    /**
     * Find all static file resources that are available from classpath.
     *
     * @param applicationArchivesBuildItem
     * @return the set of static resources
     * @throws Exception
     */
    private Set<StaticResourcesBuildItem.Entry> getClasspathResources(ApplicationArchivesBuildItem applicationArchivesBuildItem)
            throws Exception {
        Set<StaticResourcesBuildItem.Entry> knownPaths = new HashSet<>();

        ClassPathUtils.consumeAsPaths(StaticResourcesRecorder.META_INF_RESOURCES, resource -> {
            collectKnownPaths(resource, knownPaths);
        });

        for (ApplicationArchive i : applicationArchivesBuildItem.getAllApplicationArchives()) {
            i.accept(tree -> {
                Path resource = tree.getPath(StaticResourcesRecorder.META_INF_RESOURCES);
                if (resource != null && Files.exists(resource)) {
                    collectKnownPaths(resource, knownPaths);
                }
            });
        }

        return knownPaths;
    }

    private void collectKnownPaths(Path resource, Set<StaticResourcesBuildItem.Entry> knownPaths) {
        try {
            Files.walkFileTree(resource, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes attrs)
                        throws IOException {
                    String file = resource.relativize(p).toString();
                    // Windows has a backslash
                    file = file.replace('\\', '/');
                    if (!file.startsWith("/")) {
                        file = "/" + file;
                    }

                    if (QuarkusClassLoader
                            .isResourcePresentAtRuntime(StaticResourcesRecorder.META_INF_RESOURCES + file)) {
                        knownPaths.add(new StaticResourcesBuildItem.Entry(file, false));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
