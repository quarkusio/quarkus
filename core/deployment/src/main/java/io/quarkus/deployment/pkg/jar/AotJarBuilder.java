package io.quarkus.deployment.pkg.jar;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.runneraot.AotEntryPoint;
import io.quarkus.bootstrap.runneraot.AotSerializedApplication;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.jvm.ResolvedJVMRequirements;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.OpenPathTree;

public class AotJarBuilder extends AbstractLegacyThinJarBuilder<JarBuildItem> {

    private static final Logger LOG = Logger.getLogger(AotJarBuilder.class);

    private static final List<String> FULLY_INDEXED_DIRECTORIES = List.of(
            "", // root directory
            "META-INF",
            "META-INF/services");

    private final String actualMainClass;

    public AotJarBuilder(CurateOutcomeBuildItem curateOutcome,
            OutputTargetBuildItem outputTarget,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            MainClassBuildItem mainClass,
            ApplicationArchivesBuildItem applicationArchives,
            TransformedClassesBuildItem transformedClasses,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            Set<ArtifactKey> removedArtifactKeys,
            ExecutorService executorService,
            ResolvedJVMRequirements jvmRequirements) {
        super(curateOutcome, outputTarget, applicationInfo, packageConfig,
                new MainClassBuildItem(AotEntryPoint.class.getName()), applicationArchives, transformedClasses,
                generatedClasses, generatedResources, removedArtifactKeys, executorService, jvmRequirements);
        // Store the actual application main class for serialization
        this.actualMainClass = mainClass.getClassName();
    }

    public JarBuildItem build() throws IOException {
        Path runnerJar = outputTarget.getOutputDirectory()
                .resolve(outputTarget.getBaseName() + packageConfig.computedRunnerSuffix() + DOT_JAR);
        Path libDir = outputTarget.getOutputDirectory().resolve("lib");
        Path quarkusApplicationDat = outputTarget.getOutputDirectory().resolve(AotEntryPoint.QUARKUS_APPLICATION_DAT);

        Files.deleteIfExists(runnerJar);
        IoUtils.createOrEmptyDir(libDir);
        IoUtils.createOrEmptyDir(quarkusApplicationDat.getParent());

        doBuild(runnerJar, libDir);

        final Map<String, List<byte[]>> services = new HashMap<>();
        Set<String> fullyIndexedResources = new HashSet<>();

        final Collection<ResolvedDependency> runtimeDependencies = curateOutcome.getApplicationModel()
                .getRuntimeDependencies();
        final Predicate<String> ignoredEntriesPredicate = getThinJarIgnoredEntriesPredicate(packageConfig);

        ResolvedDependency appArtifact = curateOutcome.getApplicationModel().getAppArtifact();
        collectCacheableResourcesFromApplicationArchives(appArtifact, ignoredEntriesPredicate,
                services, fullyIndexedResources);

        for (ResolvedDependency runtimeDependency : runtimeDependencies) {
            // Exclude files that are not jars (typically, we can have XML files here, see https://github.com/quarkusio/quarkus/issues/2852)
            // and are not part of the optional dependencies to include
            if (!includeAppDependency(runtimeDependency, outputTarget.getIncludedOptionalDependencies(), removedArtifactKeys)) {
                continue;
            }

            collectCacheableResourcesFromApplicationArchives(runtimeDependency, ignoredEntriesPredicate,
                    services, fullyIndexedResources);
        }

        for (GeneratedClassBuildItem i : generatedClasses) {
            String fileName = fromClassNameToResourceName(i.internalName());

            if (ignoredEntriesPredicate.test(fileName)) {
                continue;
            }

            if (isDirectChildOfTrackedDirectory(fileName)) {
                fullyIndexedResources.add(fileName);
            }
        }

        for (GeneratedResourceBuildItem i : generatedResources) {
            if (ignoredEntriesPredicate.test(i.getName())) {
                continue;
            }

            if (isDirectChildOfTrackedDirectory(i.getName())) {
                fullyIndexedResources.add(i.getName());
            }

            if (isServiceFile(i.getName())) {
                services.computeIfAbsent(i.getName(), k -> new ArrayList<>())
                        .add(i.getData());
            }
        }

        Map<String, byte[]> serviceFiles = new HashMap<>();
        for (Map.Entry<String, List<byte[]>> service : services.entrySet()) {
            byte[] concatenated = concatenateServiceFiles(service.getValue());
            serviceFiles.put(service.getKey(), concatenated);
        }

        try (var out = Files.newOutputStream(quarkusApplicationDat)) {
            // For thin jars, don't claim directories are fully indexed since resources are in separate lib/ jars
            // Only cache the service files for performance
            AotSerializedApplication.write(out, actualMainClass, FULLY_INDEXED_DIRECTORIES, fullyIndexedResources,
                    serviceFiles);
        }

        // Debug logging for specific cached resources
        if (LOG.isDebugEnabled()) {
            LOG.debug("SerializedApplication created with " + serviceFiles.size() + " service files");
            LOG.debug("Collected " + fullyIndexedResources.size() + " directory entries from: " + FULLY_INDEXED_DIRECTORIES);

            serviceFiles.keySet().stream().sorted().forEach(resource -> {
                LOG.debug("  Service file: " + resource);
            });
            fullyIndexedResources.stream().sorted().forEach(entry -> {
                LOG.debug("  Directory entry: " + entry);
            });
        }

        return new JarBuildItem(runnerJar, null, libDir, packageConfig.jar().type(),
                suffixToClassifier(packageConfig.computedRunnerSuffix()));
    }

    private static void collectCacheableResourcesFromApplicationArchives(ResolvedDependency dependency,
            Predicate<String> ignoredEntriesPredicate, Map<String, List<byte[]>> services,
            Set<String> fullyIndexedResources) throws IOException {
        try (OpenPathTree pathTree = dependency.getContentTree().open()) {
            pathTree.walk(visit -> {
                try {
                    final String relativePath = visit.getRelativePath();
                    if (Files.isDirectory(visit.getPath())) {
                        return;
                    }

                    if (ignoredEntriesPredicate.test(relativePath)) {
                        return;
                    }

                    if (isDirectChildOfTrackedDirectory(relativePath)) {
                        fullyIndexedResources.add(relativePath);
                    }

                    if (isServiceFile(relativePath)) {
                        services.computeIfAbsent(relativePath, k -> new ArrayList<>())
                                .add(Files.readAllBytes(visit.getPath()));
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static boolean isDirectChildOfTrackedDirectory(String path) {
        // Check if it's a direct child of any tracked directory
        for (String trackedDir : FULLY_INDEXED_DIRECTORIES) {
            if (trackedDir.isEmpty()) {
                if (path.indexOf('/') == -1) {
                    return true;
                }
            } else {
                String prefix = trackedDir + "/";
                if (path.startsWith(prefix)) {
                    String afterPrefix = path.substring(prefix.length());
                    if (afterPrefix.indexOf('/') == -1) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isServiceFile(String resourcePath) {
        return resourcePath.startsWith("META-INF/services/") && resourcePath.length() > 18;
    }

    private static byte[] concatenateServiceFiles(List<byte[]> files) throws IOException {
        if (files.size() == 1) {
            return files.get(0);
        }

        // Concatenate with newlines between files
        var result = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < files.size(); i++) {
            result.write(files.get(i));
            if (i < files.size() - 1) {
                result.write('\n');
            }
        }
        return result.toByteArray();
    }
}
