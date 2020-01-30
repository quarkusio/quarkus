package io.quarkus.undertow.deployment;

import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.runtime.LaunchMode;

/**
 * NOTE: Shared with Resteasy standalone!
 *
 */
public class UndertowStaticResourcesBuildStep {

    protected static final String META_INF_RESOURCES_SLASH = "META-INF/resources/";
    protected static final String META_INF_RESOURCES = "META-INF/resources";

    @BuildStep
    void handleGeneratedWebResources(BuildProducer<GeneratedResourceBuildItem> generatedResources,
            List<GeneratedWebResourceBuildItem> generatedWebResources) throws Exception {
        for (GeneratedWebResourceBuildItem genResource : generatedWebResources) {
            generatedResources.produce(new GeneratedResourceBuildItem(META_INF_RESOURCES_SLASH + genResource.getName(),
                    genResource.getClassData()));
        }
    }

    @BuildStep(loadsApplicationClasses = true)
    void scanStaticResources(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<KnownPathsBuildItem> knownPathsBuilds,
            List<GeneratedWebResourceBuildItem> generatedWebResources,
            LaunchModeBuildItem launchModeBuildItem) throws Exception {

        //we need to check for web resources in order to get welcome files to work
        //this kinda sucks
        Set<String> knownFiles = new HashSet<>();
        Set<String> knownDirectories = new HashSet<>();
        for (ApplicationArchive i : applicationArchivesBuildItem.getAllApplicationArchives()) {
            Path resource = i.getChildPath(META_INF_RESOURCES);
            if (resource != null && Files.exists(resource)) {
                try (Stream<Path> fileTreeElements = Files.walk(resource)) {
                    fileTreeElements.forEach(new Consumer<Path>() {
                        @Override
                        public void accept(Path path) {
                            // Skip META-INF/resources entry
                            if (resource.equals(path)) {
                                return;
                            }
                            Path rel = resource.relativize(path);
                            if (Files.isDirectory(path)) {
                                knownDirectories.add(rel.toString());
                            } else {
                                knownFiles.add(rel.toString());
                            }
                        }
                    });
                }
            }
        }
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(META_INF_RESOURCES);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if (url.getProtocol().equals("jar")) {
                JarURLConnection jar = (JarURLConnection) url.openConnection();
                jar.setUseCaches(false);
                try (JarFile jarFile = jar.getJarFile()) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().startsWith(META_INF_RESOURCES_SLASH)) {
                            String sub = entry.getName().substring(META_INF_RESOURCES_SLASH.length());
                            if (!sub.isEmpty()) {
                                if (entry.getName().endsWith("/")) {
                                    String dir = sub.substring(0, sub.length() - 1);
                                    knownDirectories.add(dir);
                                } else {
                                    knownFiles.add(sub);
                                }
                            }
                        }
                    }
                }
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

    @BuildStep
    void nativeImageResources(KnownPathsBuildItem paths, BuildProducer<NativeImageResourceBuildItem> nativeImage) {
        for (String i : paths.knownFiles) {
            nativeImage.produce(new NativeImageResourceBuildItem(META_INF_RESOURCES_SLASH + i));
        }
    }
}
