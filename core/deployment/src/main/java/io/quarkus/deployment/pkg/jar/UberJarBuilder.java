package io.quarkus.deployment.pkg.jar;

import static io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType.UBER_JAR;

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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

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
import io.quarkus.deployment.pkg.builditem.UberJarIgnoredResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarMergedResourceBuildItem;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.sbom.ApplicationComponent;
import io.quarkus.sbom.ApplicationManifestConfig;

public class UberJarBuilder extends AbstractJarBuilder<JarBuildItem> {

    private static final Logger LOG = Logger.getLogger(UberJarBuilder.class);

    private static final Predicate<String> UBER_JAR_IGNORED_ENTRIES_PREDICATE = new IsEntryIgnoredForUberJarPredicate();

    private static final Predicate<String> UBER_JAR_CONCATENATED_ENTRIES_PREDICATE = new Predicate<>() {
        @Override
        public boolean test(String path) {
            return "META-INF/io.netty.versions.properties".equals(path) ||
                    (path.startsWith("META-INF/services/") && path.length() > 18) ||
            // needed to initialize the CLI bootstrap Maven resolver
                    "META-INF/sisu/javax.inject.Named".equals(path);
        }
    };

    private final List<UberJarMergedResourceBuildItem> mergedResources;
    private final List<UberJarIgnoredResourceBuildItem> ignoredResources;

    public UberJarBuilder(CurateOutcomeBuildItem curateOutcome,
            OutputTargetBuildItem outputTarget,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            MainClassBuildItem mainClass,
            ApplicationArchivesBuildItem applicationArchives,
            TransformedClassesBuildItem transformedClasses,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            Set<ArtifactKey> removedArtifactKeys,
            List<UberJarMergedResourceBuildItem> mergedResources,
            List<UberJarIgnoredResourceBuildItem> ignoredResources,
            ResolvedJVMRequirements jvmRequirements) {
        super(curateOutcome, outputTarget, applicationInfo, packageConfig, mainClass, applicationArchives, transformedClasses,
                generatedClasses, generatedResources, removedArtifactKeys, jvmRequirements);

        this.mergedResources = mergedResources;
        this.ignoredResources = ignoredResources;
    }

    @Override
    public JarBuildItem build() throws IOException {

        //we use the -runner jar name, unless we are building both types
        final Path runnerJar = outputTarget.getOutputDirectory()
                .resolve(outputTarget.getBaseName() + packageConfig.computedRunnerSuffix() + DOT_JAR);

        // If the runner jar appears to exist already we create a new one with a tmp suffix.
        // Deleting an existing runner jar may result in deleting the original (non-runner) jar (in case the runner suffix is empty)
        // which is used as a source of content for the runner jar.
        final Path tmpRunnerJar;
        if (Files.exists(runnerJar)) {
            tmpRunnerJar = outputTarget.getOutputDirectory()
                    .resolve(outputTarget.getBaseName() + packageConfig.computedRunnerSuffix() + ".tmp");
            Files.deleteIfExists(tmpRunnerJar);
        } else {
            tmpRunnerJar = runnerJar;
        }

        buildUberJar0(tmpRunnerJar);

        if (tmpRunnerJar != runnerJar) {
            Files.copy(tmpRunnerJar, runnerJar, StandardCopyOption.REPLACE_EXISTING);
            tmpRunnerJar.toFile().deleteOnExit();
        }

        //for uberjars we move the original jar, so there is only a single jar in the output directory
        final Path standardJar = outputTarget.getOutputDirectory()
                .resolve(outputTarget.getOriginalBaseName() + DOT_JAR);
        final Path originalJar = Files.exists(standardJar) ? standardJar : null;

        ResolvedDependency appArtifact = curateOutcome.getApplicationModel().getAppArtifact();
        final String classifier = suffixToClassifier(packageConfig.computedRunnerSuffix());
        if (classifier != null && !classifier.isEmpty()) {
            appArtifact = ResolvedDependencyBuilder.newInstance()
                    .setGroupId(appArtifact.getGroupId())
                    .setArtifactId(appArtifact.getArtifactId())
                    .setClassifier(classifier)
                    .setType(appArtifact.getType())
                    .setVersion(appArtifact.getVersion())
                    .setResolvedPaths(appArtifact.getResolvedPaths())
                    .addDependencies(appArtifact.getDependencies())
                    .setWorkspaceModule(appArtifact.getWorkspaceModule())
                    .setFlags(appArtifact.getFlags())
                    .build();
        }
        final ApplicationManifestConfig manifestConfig = ApplicationManifestConfig.builder()
                .setMainComponent(ApplicationComponent.builder()
                        .setPath(runnerJar)
                        .setResolvedDependency(appArtifact)
                        .build())
                .setRunnerPath(runnerJar)
                .addComponents(curateOutcome.getApplicationModel().getDependencies())
                .build();

        return new JarBuildItem(runnerJar, originalJar, null, UBER_JAR,
                suffixToClassifier(packageConfig.computedRunnerSuffix()), manifestConfig);
    }

    private void buildUberJar0(Path runnerJar) throws IOException {
        try (ZipFileSystemArchiveCreator archiveCreator = new ZipFileSystemArchiveCreator(runnerJar,
                packageConfig.jar().compress(), packageConfig.outputTimestamp().orElse(null))) {
            LOG.info("Building uber jar: " + runnerJar);

            final Map<String, String> seen = new HashMap<>();
            final Map<String, Set<Dependency>> duplicateCatcher = new HashMap<>();
            final Map<String, List<byte[]>> concatenatedEntries = new HashMap<>();
            final Set<String> mergeResourcePaths = mergedResources.stream()
                    .map(UberJarMergedResourceBuildItem::getPath)
                    .collect(Collectors.toSet());

            Set<String> ignoredEntries = new HashSet<>();
            packageConfig.jar().userConfiguredIgnoredEntries().ifPresent(ignoredEntries::addAll);
            ignoredResources.stream()
                    .map(UberJarIgnoredResourceBuildItem::getPath)
                    .forEach(ignoredEntries::add);
            Predicate<String> allIgnoredEntriesPredicate = new Predicate<String>() {
                @Override
                public boolean test(String path) {
                    return UBER_JAR_IGNORED_ENTRIES_PREDICATE.test(path)
                            || ignoredEntries.contains(path);
                }
            };

            ResolvedDependency appArtifact = curateOutcome.getApplicationModel().getAppArtifact();

            // the manifest needs to be the first entry in the jar, otherwise JarInputStream does not work properly
            // see https://bugs.openjdk.java.net/browse/JDK-8031748
            generateManifest(archiveCreator, "", packageConfig, appArtifact, jvmRequirements, mainClass.getClassName(),
                    applicationInfo);

            for (ResolvedDependency appDep : curateOutcome.getApplicationModel().getRuntimeDependencies()) {

                // Exclude files that are not jars (typically, we can have XML files here, see https://github.com/quarkusio/quarkus/issues/2852)
                // and are not part of the optional dependencies to include
                if (!includeAppDependency(appDep, outputTarget.getIncludedOptionalDependencies(),
                        removedArtifactKeys)) {
                    continue;
                }

                for (Path resolvedDep : appDep.getResolvedPaths()) {
                    Set<String> existingEntries = new HashSet<>();
                    Set<String> transformedFilesByJar = transformedClasses.getTransformedFilesByJar().get(resolvedDep);
                    if (transformedFilesByJar != null) {
                        existingEntries.addAll(transformedFilesByJar);
                    }
                    generatedResources.stream()
                            .map(GeneratedResourceBuildItem::getName)
                            .forEach(existingEntries::add);

                    if (!Files.isDirectory(resolvedDep)) {
                        try (FileSystem artifactFs = ZipUtils.newFileSystem(resolvedDep)) {
                            for (final Path root : artifactFs.getRootDirectories()) {
                                walkFileDependencyForDependency(root, archiveCreator, duplicateCatcher,
                                        concatenatedEntries, allIgnoredEntriesPredicate, appDep, existingEntries,
                                        mergeResourcePaths);
                            }
                        }
                    } else {
                        walkFileDependencyForDependency(resolvedDep, archiveCreator, duplicateCatcher,
                                concatenatedEntries, allIgnoredEntriesPredicate, appDep, existingEntries,
                                mergeResourcePaths);
                    }
                }
            }
            Set<Set<Dependency>> explained = new HashSet<>();
            for (Map.Entry<String, Set<Dependency>> entry : duplicateCatcher.entrySet()) {
                if (entry.getValue().size() > 1) {
                    if (explained.add(entry.getValue())) {
                        LOG.warn("Dependencies with duplicate files detected. The dependencies " + entry.getValue()
                                + " contain duplicate files, e.g. " + entry.getKey());
                    }
                }
            }
            copyCommonContent(archiveCreator, concatenatedEntries, allIgnoredEntriesPredicate);
            // now that all entries have been added, check if there's a META-INF/versions/ entry. If present,
            // mark this jar as multi-release jar. Strictly speaking, the jar spec expects META-INF/versions/N
            // directory where N is an integer greater than 8, but we don't do that level of checks here but that
            // should be OK.
            if (archiveCreator.isMultiVersion()) {
                LOG.debug("uber jar will be marked as multi-release jar");
                archiveCreator.makeMultiVersion();
            }
        }

        runnerJar.toFile().setReadable(true, false);
    }

    private void walkFileDependencyForDependency(Path root, ArchiveCreator archiveCreator,
            Map<String, Set<Dependency>> duplicateCatcher, Map<String, List<byte[]>> concatenatedEntries,
            Predicate<String> ignoredEntriesPredicate, Dependency appDep, Set<String> existingEntries,
            Set<String> mergeResourcePaths) throws IOException {
        final Path metaInfDir = root.resolve("META-INF");
        Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        final String relativePath = toUri(root.relativize(dir));
                        if (!relativePath.isEmpty()) {
                            archiveCreator.addDirectory(relativePath);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        final String relativePath = toUri(root.relativize(file));
                        //if this has been transformed we do not copy it
                        // if it's a signature file (under the <jar>/META-INF directory),
                        // then we don't add it to the uber jar
                        if (isBlockOrSF(relativePath) &&
                                file.relativize(metaInfDir).getNameCount() == 1) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Signature file " + file.toAbsolutePath() + " from app " +
                                        "dependency " + appDep + " will not be included in uberjar");
                            }
                            return FileVisitResult.CONTINUE;
                        }
                        if (!existingEntries.contains(relativePath)) {
                            if (UBER_JAR_CONCATENATED_ENTRIES_PREDICATE.test(relativePath)
                                    || mergeResourcePaths.contains(relativePath)) {
                                concatenatedEntries.computeIfAbsent(relativePath, (u) -> new ArrayList<>())
                                        .add(Files.readAllBytes(file));
                                return FileVisitResult.CONTINUE;
                            } else if (!ignoredEntriesPredicate.test(relativePath)) {
                                duplicateCatcher.computeIfAbsent(relativePath, (a) -> new HashSet<>())
                                        .add(appDep);
                                archiveCreator.addFileIfNotExists(file, relativePath, appDep.toString());
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    // same as the impl in sun.security.util.SignatureFileVerifier#isBlockOrSF()
    private static boolean isBlockOrSF(final String s) {
        if (s == null) {
            return false;
        }
        return s.endsWith(".SF")
                || s.endsWith(".DSA")
                || s.endsWith(".RSA")
                || s.endsWith(".EC");
    }

    private static class IsEntryIgnoredForUberJarPredicate implements Predicate<String> {

        private static final Set<String> UBER_JAR_IGNORED_ENTRIES = Set.of(
                "META-INF/INDEX.LIST",
                "META-INF/MANIFEST.MF",
                "module-info.class",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/LICENSE.md",
                "META-INF/LGPL-3.0.txt",
                "META-INF/ASL-2.0.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE.md",
                "META-INF/README",
                "META-INF/README.txt",
                "META-INF/README.md",
                "META-INF/DEPENDENCIES",
                "META-INF/DEPENDENCIES.txt",
                "META-INF/beans.xml",
                "META-INF/quarkus-config-roots.list",
                "META-INF/quarkus-javadoc.properties",
                "META-INF/quarkus-extension.properties",
                "META-INF/quarkus-extension.json",
                "META-INF/quarkus-extension.yaml",
                "META-INF/quarkus-deployment-dependency.graph",
                "META-INF/jandex.idx",
                "META-INF/panache-archive.marker", // deprecated and unused, but still present in some archives
                "META-INF/build.metadata", // present in the Red Hat Build of Quarkus
                "META-INF/quarkus-config-doc/quarkus-config-javadoc.json",
                "LICENSE");

        @Override
        public boolean test(String path) {
            return UBER_JAR_IGNORED_ENTRIES.contains(path)
                    || path.endsWith("module-info.class");
        }
    }
}
