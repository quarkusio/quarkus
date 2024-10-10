package io.quarkus.undertow.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.common.os.OS;

/**
 * NOTE: Shared with Resteasy standalone!
 *
 */
public class UndertowStaticResourcesBuildStep {

    protected static final String META_INF_RESOURCES_SLASH = "META-INF/resources/";
    protected static final String META_INF_RESOURCES = "META-INF/resources";

    @BuildStep
    void handleGeneratedWebResources(Capabilities capabilities, BuildProducer<GeneratedResourceBuildItem> generatedResources,
            List<GeneratedWebResourceBuildItem> generatedWebResources) throws Exception {
        if (!capabilities.isPresent(Capability.SERVLET)) {
            return;
        }
        for (GeneratedWebResourceBuildItem genResource : generatedWebResources) {
            generatedResources.produce(new GeneratedResourceBuildItem(META_INF_RESOURCES_SLASH + genResource.getName(),
                    genResource.getClassData()));
        }
    }

    @BuildStep
    void scanStaticResources(Capabilities capabilities, ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<KnownPathsBuildItem> knownPathsBuilds,
            List<GeneratedWebResourceBuildItem> generatedWebResources,
            LaunchModeBuildItem launchModeBuildItem) throws Exception {
        if (!capabilities.isPresent(Capability.SERVLET)) {
            return;
        }
        //we need to check for web resources in order to get welcome files to work
        //this kinda sucks
        final Set<String> knownFiles = new HashSet<>();
        final Set<String> knownDirectories = new HashSet<>();
        for (ApplicationArchive i : applicationArchivesBuildItem.getAllApplicationArchives()) {
            i.accept(tree -> {
                Path resource = tree.getPath(META_INF_RESOURCES);
                if (resource != null && Files.exists(resource)) {
                    collectKnownPaths(resource, knownFiles, knownDirectories);
                }
            });
        }

        for (ClassPathElement e : QuarkusClassLoader.getElements(META_INF_RESOURCES, false)) {
            if (e.isRuntime()) {
                e.apply(tree -> {
                    collectKnownPaths(tree.getPath(META_INF_RESOURCES), knownFiles, knownDirectories);
                    return null;
                });
            }
        }

        for (GeneratedWebResourceBuildItem genResource : generatedWebResources) {
            String sub = genResource.getName();
            if (sub.startsWith("/")) {
                sub = sub.substring(1);
            }
            if (!sub.isEmpty()) {
                knownFiles.add(sub);
                for (int i = 0; i < sub.length(); ++i) {
                    if (sub.charAt(i) == '/') {
                        knownDirectories.add(sub.substring(0, i));
                    }
                }
            }
        }
        if (launchModeBuildItem.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            //we don't need knownPaths in development mode
            //we serve directly from the project dir
            knownPathsBuilds.produce(new KnownPathsBuildItem(Collections.emptySet(), Collections.emptySet()));
        } else {
            knownPathsBuilds.produce(new KnownPathsBuildItem(knownFiles, knownDirectories));
        }
    }

    private void collectKnownPaths(Path resource, Set<String> knownFiles, Set<String> knownDirectories) {
        try {
            Files.walkFileTree(resource, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                        throws IOException {
                    knownFiles.add(normalizePath(resource.relativize(path).toString()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs)
                        throws IOException {
                    knownDirectories.add(normalizePath(resource.relativize(path).toString()));
                    return FileVisitResult.CONTINUE;
                }

                private String normalizePath(String path) {
                    if (OS.WINDOWS.isCurrent()) {
                        path = path.replace('\\', '/');
                    }
                    return path;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BuildStep
    void nativeImageResources(Capabilities capabilities, KnownPathsBuildItem paths,
            BuildProducer<NativeImageResourceBuildItem> nativeImage) {
        if (!capabilities.isPresent(Capability.SERVLET)) {
            return;
        }
        for (String i : paths.knownFiles) {
            nativeImage.produce(new NativeImageResourceBuildItem(META_INF_RESOURCES_SLASH + i));
        }
    }
}
