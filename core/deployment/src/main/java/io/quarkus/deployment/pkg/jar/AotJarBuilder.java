package io.quarkus.deployment.pkg.jar;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.runneraot.AotEntryPoint;
import io.quarkus.bootstrap.runneraot.AotSerializedCache;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.jvm.ResolvedJVMRequirements;
import io.quarkus.deployment.pkg.JarUnsigner;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Jar builder for AOT-optimized packaging.
 *
 * This builder creates a jar package optimized for Ahead-of-Time (AOT) compilation
 * with Java 25 and Project Leyden. It uses standard Java classloaders for maximum
 * AOT compatibility while maintaining a thin caching layer for frequently-accessed
 * resources like service files and configuration files.
 *
 * Key characteristics:
 * - Traditional jar structure (similar to legacy-jar)
 * - Standard Java classloading for classes (no custom classloader)
 * - Cached resource lookups for service files and properties
 * - Optimal performance with AOT compilation
 */
public class AotJarBuilder extends AbstractJarBuilder<JarBuildItem> {

    private static final Logger LOG = Logger.getLogger(AotJarBuilder.class);

    private static final String AOT_RESOURCE_CACHE_DAT = "aot-resource-cache.dat";
    private static final String MP_CONFIG_FILE = "META-INF/microprofile-config.properties";
    private static final String APP_PROPERTIES_FILE = "application.properties";

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
        super(curateOutcome, outputTarget, applicationInfo, packageConfig, mainClass, applicationArchives, transformedClasses,
                generatedClasses, generatedResources, removedArtifactKeys, executorService, jvmRequirements);
    }

    @Override
    public JarBuildItem build() throws IOException {
        Path runnerJar = outputTarget.getOutputDirectory()
                .resolve(outputTarget.getBaseName() + packageConfig.computedRunnerSuffix() + DOT_JAR);
        Path libDir = outputTarget.getOutputDirectory().resolve("lib");
        Path quarkusDir = outputTarget.getOutputDirectory().resolve("quarkus");

        Files.deleteIfExists(runnerJar);
        IoUtils.createOrEmptyDir(libDir);
        IoUtils.createOrEmptyDir(quarkusDir);

        LOG.info("Building AOT-optimized jar: " + runnerJar);

        // Collect cacheable resources from all jars
        Map<String, byte[]> cachedResources = new HashMap<>();

        // Build the jar
        try (ArchiveCreator archiveCreator = new ParallelCommonsCompressArchiveCreator(runnerJar,
                packageConfig.jar().compress(), packageConfig.outputTimestamp().orElse(null), outputTarget.getOutputDirectory(),
                executorService)) {

            final Map<String, String> seen = new HashMap<>();
            final StringBuilder classPath = new StringBuilder();
            final Map<String, List<byte[]>> services = new HashMap<>();

            final Collection<ResolvedDependency> appDeps = curateOutcome.getApplicationModel()
                    .getRuntimeDependencies();

            Predicate<String> ignoredEntriesPredicate = getThinJarIgnoredEntriesPredicate(packageConfig);

            // Copy library jars and collect cacheable resources
            copyLibraryJarsAndCollectResources(outputTarget, transformedClasses, libDir, classPath, appDeps,
                    services, ignoredEntriesPredicate, removedArtifactKeys, cachedResources);

            ResolvedDependency appArtifact = curateOutcome.getApplicationModel().getAppArtifact();

            // Generate manifest with main class pointing to AotEntryPoint
            generateManifest(archiveCreator, classPath.toString(), packageConfig, appArtifact, jvmRequirements,
                    AotEntryPoint.class.getName(), applicationInfo);

            // Copy common content (this also populates services map with generated service files)
            copyCommonContent(archiveCreator, services, ignoredEntriesPredicate);

            // Collect cacheable resources from application's own archives
            collectCacheableResourcesFromApplicationArchives(services, cachedResources, ignoredEntriesPredicate);

            // Collect cacheable resources from generated resources (non-service files only)
            for (GeneratedResourceBuildItem resource : generatedResources) {
                String resourceName = resource.getName();
                if (isServiceFile(resourceName)) {
                    // Service files are already handled by copyCommonContent -> services map
                    // They will be concatenated below
                    continue;
                } else if (isNonServiceCacheableResource(resourceName)) {
                    cachedResources.put(resourceName, resource.getData());
                }
            }

            // Concatenate and cache all collected service files
            for (Map.Entry<String, List<byte[]>> service : services.entrySet()) {
                byte[] concatenated = concatenateServiceFiles(service.getValue());
                cachedResources.put(service.getKey(), concatenated);
            }
        }

        // Write the cached resources file with main class
        Path cacheFile = quarkusDir.resolve(AOT_RESOURCE_CACHE_DAT);
        try (var out = Files.newOutputStream(cacheFile)) {
            AotSerializedCache.write(out, mainClass.getClassName(), cachedResources);
        }

        // Log summary of what was cached
        long serviceFileCount = cachedResources.keySet().stream()
                .filter(this::isServiceFile)
                .count();
        long otherResourceCount = cachedResources.size() - serviceFileCount;

        LOG.info("AOT cache created with " + cachedResources.size() + " resources: "
                + serviceFileCount + " service files, "
                + otherResourceCount + " configuration files");

        // Debug logging for specific cached resources
        if (LOG.isDebugEnabled()) {
            cachedResources.keySet().stream().sorted().forEach(resource -> {
                LOG.debug("  Cached: " + resource);
            });
        }

        runnerJar.toFile().setReadable(true, false);

        return new JarBuildItem(runnerJar, null, libDir, packageConfig.jar().type(),
                suffixToClassifier(packageConfig.computedRunnerSuffix()));
    }

    /**
     * Copies library jars to the lib directory and collects cacheable resources.
     *
     * Strategy:
     * - Service files (META-INF/services/*) go into services map for concatenation
     * - Other cacheable resources go directly into cachedResources map
     */
    private void copyLibraryJarsAndCollectResources(OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses, Path libDir,
            StringBuilder classPath, Collection<ResolvedDependency> appDeps, Map<String, List<byte[]>> services,
            Predicate<String> ignoredEntriesPredicate, Set<ArtifactKey> removedDependencies,
            Map<String, byte[]> cachedResources) throws IOException {

        for (ResolvedDependency appDep : appDeps) {
            if (!includeAppDependency(appDep, outputTargetBuildItem.getIncludedOptionalDependencies(), removedDependencies)) {
                continue;
            }

            for (Path resolvedDep : appDep.getResolvedPaths()) {
                if (!Files.isDirectory(resolvedDep)) {
                    Set<String> transformedFromThisArchive = transformedClasses.getTransformedFilesByJar().get(resolvedDep);
                    final String fileName;
                    final Path targetPath;

                    if (transformedFromThisArchive == null || transformedFromThisArchive.isEmpty()) {
                        fileName = appDep.getGroupId() + "." + resolvedDep.getFileName();
                        targetPath = libDir.resolve(fileName);
                        JarUnsigner.unsignJar(resolvedDep, targetPath);
                    } else {
                        fileName = "modified-" + appDep.getGroupId() + "." + resolvedDep.getFileName();
                        targetPath = libDir.resolve(fileName);
                        JarUnsigner.unsignJar(resolvedDep, targetPath, Predicate.not(transformedFromThisArchive::contains));
                    }

                    classPath.append(" lib/").append(fileName);

                    // Collect cacheable resources from this jar
                    collectCacheableResourcesFromJar(resolvedDep, services, cachedResources);
                } else {
                    // Handle directory-based dependencies (for local development)
                    collectCacheableResourcesFromDirectory(resolvedDep, services, cachedResources, ignoredEntriesPredicate);
                }
            }
        }
    }

    /**
     * Collects cacheable resources from a jar file.
     * Service files are collected separately for concatenation.
     */
    private void collectCacheableResourcesFromJar(Path jarPath, Map<String, List<byte[]>> services,
            Map<String, byte[]> cachedResources) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();

                // Service files need special handling - they can appear in multiple jars
                if (isServiceFile(entryName)) {
                    try (var in = jarFile.getInputStream(entry)) {
                        services.computeIfAbsent(entryName, k -> new ArrayList<>()).add(in.readAllBytes());
                    }
                } else if (isNonServiceCacheableResource(entryName)) {
                    // Other cacheable resources (properties files, etc.)
                    try (var in = jarFile.getInputStream(entry)) {
                        // First occurrence wins - typical classloader behavior
                        cachedResources.putIfAbsent(entryName, in.readAllBytes());
                    }
                }
            }
        }
    }

    /**
     * Collects cacheable resources from a directory (for local development).
     */
    private void collectCacheableResourcesFromDirectory(Path directory, Map<String, List<byte[]>> services,
            Map<String, byte[]> cachedResources, Predicate<String> ignoredEntriesPredicate) throws IOException {
        Files.walkFileTree(directory, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path relativePath = directory.relativize(file);
                        String relativeUri = toUri(relativePath);

                        if (ignoredEntriesPredicate.test(relativeUri)) {
                            return FileVisitResult.CONTINUE;
                        }

                        // Service files go into services map for concatenation
                        if (isServiceFile(relativeUri)) {
                            services.computeIfAbsent(relativeUri, k -> new ArrayList<>())
                                    .add(Files.readAllBytes(file));
                        } else if (isNonServiceCacheableResource(relativeUri)) {
                            // Other cacheable resources
                            cachedResources.putIfAbsent(relativeUri, Files.readAllBytes(file));
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    /**
     * Collects cacheable resources from the application's own archives.
     * This ensures application.properties and other config files from the app itself are cached.
     */
    private void collectCacheableResourcesFromApplicationArchives(Map<String, List<byte[]>> services,
            Map<String, byte[]> cachedResources, Predicate<String> ignoredEntriesPredicate) throws IOException {
        for (var archive : applicationArchives.getAllApplicationArchives()) {
            for (Path archivePath : archive.getResolvedPaths()) {
                if (Files.isDirectory(archivePath)) {
                    collectCacheableResourcesFromDirectory(archivePath, services, cachedResources, ignoredEntriesPredicate);
                } else {
                    collectCacheableResourcesFromJar(archivePath, services, cachedResources);
                }
            }
        }
    }

    /**
     * Checks if a resource is a service file.
     */
    private boolean isServiceFile(String resourcePath) {
        return resourcePath.startsWith("META-INF/services/") && resourcePath.length() > 18;
    }

    /**
     * Checks if a resource should be cached (excluding service files).
     * Service files are handled separately since they need concatenation.
     */
    private boolean isNonServiceCacheableResource(String resourcePath) {
        return resourcePath.equals(MP_CONFIG_FILE)
                || resourcePath.equals(APP_PROPERTIES_FILE);
    }

    /**
     * Concatenates multiple service files into a single byte array.
     */
    private byte[] concatenateServiceFiles(List<byte[]> files) throws IOException {
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

    private static Predicate<String> getThinJarIgnoredEntriesPredicate(PackageConfig packageConfig) {
        return packageConfig.jar().userConfiguredIgnoredEntries().map(Set::copyOf).orElse(Set.of())::contains;
    }
}
