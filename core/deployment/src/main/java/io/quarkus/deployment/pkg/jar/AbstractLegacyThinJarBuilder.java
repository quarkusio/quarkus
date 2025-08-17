package io.quarkus.deployment.pkg.jar;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.JarUnsigner;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;

public abstract class AbstractLegacyThinJarBuilder<T extends BuildItem> extends AbstractJarBuilder<T> {

    private static final Logger LOG = Logger.getLogger(AbstractLegacyThinJarBuilder.class);

    protected final CurateOutcomeBuildItem curateOutcome;
    protected final OutputTargetBuildItem outputTarget;
    protected final ApplicationInfoBuildItem applicationInfo;
    protected final PackageConfig packageConfig;
    protected final MainClassBuildItem mainClass;
    protected final ApplicationArchivesBuildItem applicationArchives;
    protected final TransformedClassesBuildItem transformedClasses;
    protected final List<GeneratedClassBuildItem> generatedClasses;
    protected final List<GeneratedResourceBuildItem> generatedResources;
    protected final Set<ArtifactKey> removedArtifactKeys;

    public AbstractLegacyThinJarBuilder(CurateOutcomeBuildItem curateOutcome,
            OutputTargetBuildItem outputTarget,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            MainClassBuildItem mainClass,
            ApplicationArchivesBuildItem applicationArchives,
            TransformedClassesBuildItem transformedClasses,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            Set<ArtifactKey> removedArtifactKeys) {
        this.curateOutcome = curateOutcome;
        this.outputTarget = outputTarget;
        this.applicationInfo = applicationInfo;
        this.packageConfig = packageConfig;
        this.mainClass = mainClass;
        this.applicationArchives = applicationArchives;
        this.transformedClasses = transformedClasses;
        this.generatedClasses = generatedClasses;
        this.generatedResources = generatedResources;
        this.removedArtifactKeys = removedArtifactKeys;
    }

    public abstract T build() throws IOException;

    protected void doBuild(Path runnerJar, Path libDir) throws IOException {
        try (FileSystem runnerZipFs = createNewZip(runnerJar, packageConfig)) {
            final Map<String, String> seen = new HashMap<>();
            final StringBuilder classPath = new StringBuilder();
            final Map<String, List<byte[]>> services = new HashMap<>();

            final Collection<ResolvedDependency> appDeps = curateOutcome.getApplicationModel()
                    .getRuntimeDependencies();

            Predicate<String> ignoredEntriesPredicate = getThinJarIgnoredEntriesPredicate(packageConfig);

            copyLibraryJars(runnerZipFs, outputTarget, transformedClasses, libDir, classPath, appDeps, services,
                    ignoredEntriesPredicate, removedArtifactKeys);

            ResolvedDependency appArtifact = curateOutcome.getApplicationModel().getAppArtifact();
            // the manifest needs to be the first entry in the jar, otherwise JarInputStream does not work properly
            // see https://bugs.openjdk.java.net/browse/JDK-8031748
            generateManifest(runnerZipFs, classPath.toString(), packageConfig, appArtifact, mainClass.getClassName(),
                    applicationInfo);

            copyCommonContent(runnerZipFs, services, applicationArchives, transformedClasses, generatedClasses,
                    generatedResources, seen, ignoredEntriesPredicate);
        }
        runnerJar.toFile().setReadable(true, false);
    }

    private static void copyLibraryJars(FileSystem runnerZipFs, OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses, Path libDir,
            StringBuilder classPath, Collection<ResolvedDependency> appDeps, Map<String, List<byte[]>> services,
            Predicate<String> ignoredEntriesPredicate, Set<ArtifactKey> removedDependencies) throws IOException {
        for (ResolvedDependency appDep : appDeps) {

            // Exclude files that are not jars (typically, we can have XML files here, see https://github.com/quarkusio/quarkus/issues/2852)
            // and are not part of the optional dependencies to include
            if (!includeAppDependency(appDep, outputTargetBuildItem.getIncludedOptionalDependencies(), removedDependencies)) {
                continue;
            }

            for (Path resolvedDep : appDep.getResolvedPaths()) {
                if (!Files.isDirectory(resolvedDep)) {
                    Set<String> transformedFromThisArchive = transformedClasses.getTransformedFilesByJar().get(resolvedDep);
                    if (transformedFromThisArchive == null || transformedFromThisArchive.isEmpty()) {
                        final String fileName = appDep.getGroupId() + "." + resolvedDep.getFileName();
                        final Path targetPath = libDir.resolve(fileName);
                        // Unsign the jar before copying it
                        JarUnsigner.unsignJar(resolvedDep, targetPath);
                        classPath.append(" lib/").append(fileName);
                    } else {
                        //we have transformed classes, we need to handle them correctly
                        final String fileName = "modified-" + appDep.getGroupId() + "."
                                + resolvedDep.getFileName();
                        final Path targetPath = libDir.resolve(fileName);
                        classPath.append(" lib/").append(fileName);
                        JarUnsigner.unsignJar(resolvedDep, targetPath, Predicate.not(transformedFromThisArchive::contains));
                    }
                } else {
                    // This case can happen when we are building a jar from inside the Quarkus repository
                    // and Quarkus Bootstrap's localProjectDiscovery has been set to true. In such a case
                    // the non-jar dependencies are the Quarkus dependencies picked up on the file system
                    Files.walkFileTree(resolvedDep, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                            new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                        throws IOException {
                                    final Path relativePath = resolvedDep.relativize(file);
                                    final String relativeUri = toUri(relativePath);
                                    if (ignoredEntriesPredicate.test(relativeUri)) {
                                        return FileVisitResult.CONTINUE;
                                    }
                                    if (relativeUri.startsWith("META-INF/services/") && relativeUri.length() > 18) {
                                        services.computeIfAbsent(relativeUri, (u) -> new ArrayList<>())
                                                .add(Files.readAllBytes(file));
                                    } else if (file.getFileName().toString().endsWith(".class")) {
                                        final Path targetPath = runnerZipFs.getPath(relativePath.toString());
                                        if (targetPath.getParent() != null) {
                                            Files.createDirectories(targetPath.getParent());
                                        }
                                        Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING); //replace only needed for testing
                                    }
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                }
            }
        }
    }

    private static Predicate<String> getThinJarIgnoredEntriesPredicate(PackageConfig packageConfig) {
        return packageConfig.jar().userConfiguredIgnoredEntries().map(Set::copyOf).orElse(Set.of())::contains;
    }
}
