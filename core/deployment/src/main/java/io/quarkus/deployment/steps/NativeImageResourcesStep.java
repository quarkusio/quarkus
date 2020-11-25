package io.quarkus.deployment.steps;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceDirectoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem.Builder;
import io.quarkus.deployment.pkg.NativeConfig;

public class NativeImageResourcesStep {

    @BuildStep
    List<NativeImageResourceBuildItem> registerPackageResources(
            List<NativeImageResourceDirectoryBuildItem> nativeImageResourceDirectories)
            throws IOException, URISyntaxException {
        List<NativeImageResourceBuildItem> resources = new ArrayList<>();

        for (NativeImageResourceDirectoryBuildItem nativeImageResourceDirectory : nativeImageResourceDirectories) {
            String path = Thread.currentThread().getContextClassLoader().getResource(nativeImageResourceDirectory.getPath())
                    .getPath();
            File resourceFile = Paths.get(new URL(path.substring(0, path.indexOf("!"))).toURI()).toFile();
            try (JarFile jarFile = new JarFile(resourceFile)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String resourceName = entry.getName();
                    if (!entry.isDirectory() && resourceName.startsWith(nativeImageResourceDirectory.getPath())
                            && !resourceName.endsWith(".class")) {
                        resources.add(new NativeImageResourceBuildItem(resourceName));
                    }
                }
            }
        }

        return resources;
    }

    @BuildStep
    void forwardResourcePatternConfigToBuildItem(
            NativeConfig nativeConfig,
            BuildProducer<NativeImageResourcePatternsBuildItem> nativeImageResourcePatterns) {

        final Optional<List<String>> includes = nativeConfig.resources.includes;
        if (includes.isPresent()) {
            final Builder builder = NativeImageResourcePatternsBuildItem.builder();
            includes.ifPresent(builder::includeGlobs);
            nativeImageResourcePatterns.produce(builder.build());
        }
    }
}
