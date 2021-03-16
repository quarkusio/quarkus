package io.quarkus.vertx.http.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.runtime.StaticResourcesRecorder;

/**
 * Handles all static file resources found in {@code META-INF/resources} unless the servlet container is present.
 */
public class StaticResourcesProcessor {

    public static final class StaticResourcesBuildItem extends SimpleBuildItem {

        private final Set<String> paths;

        public StaticResourcesBuildItem(Set<String> paths) {
            this.paths = paths;
        }

        public Set<String> getPaths() {
            return paths;
        }

    }

    @BuildStep
    void collectStaticResources(Capabilities capabilities, ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<StaticResourcesBuildItem> staticResources) throws Exception {
        if (capabilities.isPresent(Capability.SERVLET)) {
            // Servlet container handles static resources
            return;
        }
        Set<String> paths = getClasspathResources(applicationArchivesBuildItem);
        if (!paths.isEmpty()) {
            staticResources.produce(new StaticResourcesBuildItem(paths));
        }
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void staticInit(Optional<StaticResourcesBuildItem> staticResources,
            StaticResourcesRecorder recorder) throws Exception {
        if (staticResources.isPresent()) {
            recorder.staticInit(staticResources.get().getPaths());
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void runtimeInit(Optional<StaticResourcesBuildItem> staticResources, StaticResourcesRecorder recorder,
            CoreVertxBuildItem vertx, BeanContainerBuildItem beanContainer, BuildProducer<DefaultRouteBuildItem> defaultRoutes)
            throws Exception {
        if (staticResources.isPresent()) {
            defaultRoutes.produce(new DefaultRouteBuildItem(recorder.start()));
        }
    }

    /**
     * Find all static file resources that are available from classpath.
     *
     * @param applicationArchivesBuildItem
     * @return the set of static resources
     * @throws Exception
     */
    private Set<String> getClasspathResources(ApplicationArchivesBuildItem applicationArchivesBuildItem) throws Exception {
        Set<String> knownPaths = new HashSet<>();
        for (ApplicationArchive i : applicationArchivesBuildItem.getAllApplicationArchives()) {
            Path resource = i.getChildPath(StaticResourcesRecorder.META_INF_RESOURCES);
            if (resource != null && Files.exists(resource)) {
                collectKnownPaths(resource, knownPaths);
            }
        }

        ClassPathUtils.consumeAsPaths(StaticResourcesRecorder.META_INF_RESOURCES, resource -> {
            collectKnownPaths(resource, knownPaths);
        });

        return knownPaths;
    }

    private void collectKnownPaths(Path resource, Set<String> knownPaths) {
        try {
            Files.walkFileTree(resource, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes attrs)
                        throws IOException {
                    String simpleName = p.getFileName().toString();
                    String file = resource.relativize(p).toString();
                    if (simpleName.equals("index.html") || simpleName.equals("index.htm")) {
                        Path parent = resource.relativize(p).getParent();
                        if (parent == null) {
                            knownPaths.add("/");
                        } else {
                            String parentString = parent.toString();
                            if (!parentString.startsWith("/")) {
                                parentString = "/" + parentString;
                            }
                            knownPaths.add(parentString + "/");
                        }
                    }
                    if (!file.startsWith("/")) {
                        file = "/" + file;
                    }
                    // Windows has a backslash
                    file = file.replace('\\', '/');
                    knownPaths.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
