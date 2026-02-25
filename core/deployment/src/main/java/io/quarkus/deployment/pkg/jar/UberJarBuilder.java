package io.quarkus.deployment.pkg.jar;

import static io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType.UBER_JAR;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.jar.Manifest;
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
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.sbom.ApplicationComponent;
import io.quarkus.sbom.ApplicationManifestConfig;

public class UberJarBuilder extends AbstractJarBuilder<JarBuildItem> {

    private static final Logger LOG = Logger.getLogger(UberJarBuilder.class);

    private static final Predicate<String> UBER_JAR_IGNORED_ENTRIES_PREDICATE = new IsEntryIgnoredForUberJarPredicate();

    private static final Predicate<String> UBER_JAR_IGNORED_DUPLICATE_ENTRIES_PREDICATE = new IsDuplicateEntryIgnoredForUberJarPredicate();

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
            ExecutorService executorService,
            ResolvedJVMRequirements jvmRequirements) {
        super(curateOutcome, outputTarget, applicationInfo, packageConfig, mainClass, applicationArchives, transformedClasses,
                generatedClasses, generatedResources, removedArtifactKeys, executorService, jvmRequirements);

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

        try (ArchiveCreator archiveCreator = new ParallelCommonsCompressArchiveCreator(runnerJar,
                packageConfig.jar().compress(), packageConfig.outputTimestamp().orElse(null),
                executorService)) {
            LOG.info("Building uber jar: " + runnerJar);

            final Map<String, Set<Dependency>> duplicateCatcher = new HashMap<>();
            final Map<String, List<byte[]>> concatenatedEntries = new HashMap<>();
            final Set<String> mergeResourcePaths = mergedResources.stream()
                    .map(UberJarMergedResourceBuildItem::getPath)
                    .collect(Collectors.toSet());

            final Predicate<String> allIgnoredEntriesPredicate = getIgnoredEntriesPredicate();

            ResolvedDependency appArtifact = curateOutcome.getApplicationModel().getAppArtifact();

            // the manifest needs to be the first entry in the jar, otherwise JarInputStream does not work properly
            // see https://bugs.openjdk.java.net/browse/JDK-8031748
            // the ArchiveCreator now makes sure it's the case, keeping the comment in case we change the implementation at some point
            Manifest manifest = createManifest(packageConfig, appArtifact, applicationInfo);
            attachRunnerMetadata(manifest, mainClass.getClassName(), "", jvmRequirements);
            archiveCreator.addManifest(manifest);

            final Set<String> existingEntries = new HashSet<>();
            generatedResources.stream()
                    .map(GeneratedResourceBuildItem::getName)
                    .forEach(existingEntries::add);

            for (ResolvedDependency appDep : curateOutcome.getApplicationModel().getRuntimeDependencies()) {

                // Exclude files that are not jars (typically, we can have XML files here, see https://github.com/quarkusio/quarkus/issues/2852)
                // and are not part of the optional dependencies to include
                if (!includeAppDependency(appDep, outputTarget.getIncludedOptionalDependencies(), removedArtifactKeys)) {
                    continue;
                }

                for (Path resolvedDep : appDep.getResolvedPaths()) {
                    Set<String> transformedFilesByJar = transformedClasses.getTransformedFilesByJar().get(resolvedDep);
                    if (transformedFilesByJar != null) {
                        existingEntries.addAll(transformedFilesByJar);
                    }
                }

                walkFileDependencyForDependency(archiveCreator, duplicateCatcher,
                        concatenatedEntries, allIgnoredEntriesPredicate, appDep, existingEntries,
                        mergeResourcePaths);
            }

            Map<Set<Dependency>, List<String>> explained = new HashMap<>();
            for (Map.Entry<String, Set<Dependency>> entry : duplicateCatcher.entrySet()) {
                if (entry.getValue().size() > 1) {
                    explained.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
                }
            }
            if (!explained.isEmpty()) {
                for (Map.Entry<Set<Dependency>, List<String>> entry : explained.entrySet()) {
                    var msg = new StringBuilder().append("Dependencies:");
                    for (var dep : entry.getKey()) {
                        msg.append(System.lineSeparator()).append("- ").append(dep.toCompactCoords());
                    }
                    msg.append(System.lineSeparator()).append("contain duplicate files:");
                    for (var path : entry.getValue()) {
                        msg.append(System.lineSeparator()).append("- ").append(path);
                    }
                    LOG.warn(msg);
                }
            }

            copyApplicationContent(archiveCreator, concatenatedEntries, allIgnoredEntriesPredicate);
            // now that all entries have been added, check if there's a META-INF/versions/ entry. If present,
            // mark this jar as multi-release jar. Strictly speaking, the jar spec expects META-INF/versions/N
            // directory where N is an integer greater than 8, but we don't do that level of checks here but that
            // should be OK.
            if (archiveCreator.isMultiVersion()) {
                LOG.debug("Uber jar will be marked as multi-release jar");
                archiveCreator.makeMultiVersion();
            }
        }

        runnerJar.toFile().setReadable(true, false);
    }

    private Predicate<String> getIgnoredEntriesPredicate() {
        if (packageConfig.jar().userConfiguredIgnoredEntries().isEmpty() && ignoredResources.isEmpty()) {
            return UBER_JAR_IGNORED_ENTRIES_PREDICATE;
        }

        final List<String> userIgnoredEntries = packageConfig.jar().userConfiguredIgnoredEntries().orElse(List.of());
        final Set<String> ignoredEntries = new HashSet<>(userIgnoredEntries.size() + ignoredResources.size());
        ignoredEntries.addAll(userIgnoredEntries);
        for (var ignoredResource : ignoredResources) {
            ignoredEntries.add(ignoredResource.getPath());
        }
        return path -> UBER_JAR_IGNORED_ENTRIES_PREDICATE.test(path) || ignoredEntries.contains(path);
    }

    private void walkFileDependencyForDependency(ArchiveCreator archiveCreator,
            Map<String, Set<Dependency>> duplicateCatcher, Map<String, List<byte[]>> concatenatedEntries,
            Predicate<String> ignoredEntriesPredicate, ResolvedDependency appDep, Set<String> existingEntries,
            Set<String> mergeResourcePaths) throws IOException {

        // The reason opening and closing a path tree right away works, unlike creating and closing a ZipFileSystem,
        // is that we are actually using a SharedOpenArchivePathTree here, which simply increments and decrements
        // the user count of the cached shared open path tree instance. This open path tree instance will remain open
        // until the last user called close() on it, which will be the Quarkus classloaders closing.
        try (OpenPathTree pathTree = appDep.getContentTree().open()) {
            pathTree.walkRaw(visit -> {
                try {
                    final String relativePath = visit.getRelativePath();
                    if (Files.isDirectory(visit.getPath())) {
                        if (!relativePath.isEmpty()) {
                            archiveCreator.addDirectory(relativePath);
                        }
                        return;
                    }

                    final Path file = visit.getPath();
                    //if this has been transformed we do not copy it
                    // if it's a signature file (under the <jar>/META-INF directory),
                    // then we don't add it to the uber jar
                    if (isBlockOrSF(relativePath) &&
                            relativePath.startsWith("META-INF/") && relativePath.indexOf('/', "META-INF/".length()) < 0) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Signature file " + file.toAbsolutePath() + " from app " +
                                    "dependency " + appDep + " will not be included in uberjar");
                        }
                        return;
                    }

                    if (!existingEntries.contains(relativePath)) {
                        if (UBER_JAR_CONCATENATED_ENTRIES_PREDICATE.test(relativePath)
                                || mergeResourcePaths.contains(relativePath)) {
                            concatenatedEntries.computeIfAbsent(relativePath, (u) -> new ArrayList<>())
                                    .add(Files.readAllBytes(file));
                        } else if (!ignoredEntriesPredicate.test(relativePath)) {
                            if (!UBER_JAR_IGNORED_DUPLICATE_ENTRIES_PREDICATE.test(relativePath)) {
                                duplicateCatcher.computeIfAbsent(relativePath, (a) -> new HashSet<>())
                                        .add(appDep);
                            }
                            archiveCreator.addFileIfNotExists(file, relativePath, appDep.toString());
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
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
                "META-INF/quarkus-config-doc/quarkus-config-model-version",
                "META-INF/quarkus-config-doc/quarkus-config-model.json",
                "LICENSE");

        @Override
        public boolean test(String path) {
            return UBER_JAR_IGNORED_ENTRIES.contains(path)
                    || path.endsWith("module-info.class");
        }
    }

    /**
     * When this predicate is true, the entry will be added to the jar, but we won't log any warning if there is a duplicate.
     */
    private static class IsDuplicateEntryIgnoredForUberJarPredicate implements Predicate<String> {

        @Override
        public boolean test(String path) {
            return path.startsWith("META-INF/maven/");
        }
    }
}
