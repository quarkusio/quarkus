package io.quarkus.deployment.pkg.steps;

import static io.quarkus.fs.util.ZipUtils.wrapForJDK8232879;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.MutableJarApplicationModel;
import io.quarkus.bootstrap.runner.QuarkusEntryPoint;
import io.quarkus.bootstrap.runner.SerializedApplication;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.QuarkusBuildCloseablesBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.configuration.ClassLoadingConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.AppCDSRequestedBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.LegacyJarRequiredBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarIgnoredResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarMergedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarRequiredBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathVisit;
import io.quarkus.paths.PathVisitor;
import io.quarkus.utilities.JavaBinFinder;

/**
 * This build step builds both the thin jars and uber jars.
 *
 * The way this is built is a bit convoluted. In general, we only want a single one built,
 * as determined by the {@link PackageConfig} (unless the config explicitly asks for both of them)
 *
 * However, we still need an extension to be able to ask for a specific one of these despite the config,
 * e.g. if a serverless environment needs an uberjar to build its deployment package then we need
 * to be able to provide this.
 *
 * To enable this we have two build steps that strongly produce the respective artifact type build
 * items, but not a {@link ArtifactResultBuildItem}. We then
 * have another two build steps that only run if they are configured to consume these explicit
 * build items and transform them into {@link ArtifactResultBuildItem}.
 */
public class JarResultBuildStep {

    private static final String DOT_JAR = ".jar";

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

    private static final Logger log = Logger.getLogger(JarResultBuildStep.class);

    private static final BiPredicate<Path, BasicFileAttributes> IS_JSON_FILE_PREDICATE = new IsJsonFilePredicate();

    public static final String DEPLOYMENT_CLASS_PATH_DAT = "deployment-class-path.dat";

    public static final String BUILD_SYSTEM_PROPERTIES = "build-system.properties";

    public static final String DEPLOYMENT_LIB = "deployment";

    public static final String APPMODEL_DAT = "appmodel.dat";

    public static final String QUARKUS_RUN_JAR = "quarkus-run.jar";

    public static final String QUARKUS_APP_DEPS = "quarkus-app-dependencies.txt";

    public static final String BOOT_LIB = "boot";

    public static final String LIB = "lib";

    public static final String MAIN = "main";

    public static final String GENERATED_BYTECODE_JAR = "generated-bytecode.jar";

    public static final String TRANSFORMED_BYTECODE_JAR = "transformed-bytecode.jar";

    public static final String APP = "app";

    public static final String QUARKUS = "quarkus";

    public static final String DEFAULT_FAST_JAR_DIRECTORY_NAME = "quarkus-app";

    public static final String MP_CONFIG_FILE = "META-INF/microprofile-config.properties";
    private static final String VINEFLOWER_VERSION = "1.9.3";

    @BuildStep
    OutputTargetBuildItem outputTarget(BuildSystemTargetBuildItem bst, PackageConfig packageConfig) {
        String name = packageConfig.outputName.orElseGet(bst::getBaseName);
        Path path = packageConfig.outputDirectory.map(s -> bst.getOutputDirectory().resolve(s))
                .orElseGet(bst::getOutputDirectory);
        Optional<Set<ArtifactKey>> includedOptionalDependencies;
        if (packageConfig.filterOptionalDependencies) {
            includedOptionalDependencies = Optional.of(packageConfig.includedOptionalDependencies
                    .map(set -> set.stream().map(s -> ArtifactKey.fromString(s)).collect(Collectors.toSet()))
                    .orElse(Collections.emptySet()));
        } else {
            includedOptionalDependencies = Optional.empty();
        }
        return new OutputTargetBuildItem(path, name, bst.getOriginalBaseName(), bst.isRebuild(), bst.getBuildSystemProps(),
                includedOptionalDependencies);
    }

    @BuildStep(onlyIf = JarRequired.class)
    ArtifactResultBuildItem jarOutput(JarBuildItem jarBuildItem) {
        if (jarBuildItem.getLibraryDir() != null) {
            return new ArtifactResultBuildItem(jarBuildItem.getPath(), PackageConfig.JAR,
                    Collections.singletonMap("library-dir", jarBuildItem.getLibraryDir().toString()));
        } else {
            return new ArtifactResultBuildItem(jarBuildItem.getPath(), PackageConfig.JAR, Collections.emptyMap());
        }
    }

    @BuildStep
    public JarBuildItem buildRunnerJar(CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            ClassLoadingConfig classLoadingConfig,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            List<UberJarRequiredBuildItem> uberJarRequired,
            List<UberJarMergedResourceBuildItem> uberJarMergedResourceBuildItems,
            List<UberJarIgnoredResourceBuildItem> uberJarIgnoredResourceBuildItems,
            List<LegacyJarRequiredBuildItem> legacyJarRequired,
            QuarkusBuildCloseablesBuildItem closeablesBuildItem,
            List<AdditionalApplicationArchiveBuildItem> additionalApplicationArchiveBuildItems,
            MainClassBuildItem mainClassBuildItem, Optional<AppCDSRequestedBuildItem> appCDS) throws Exception {

        if (appCDS.isPresent()) {
            handleAppCDSSupportFileGeneration(transformedClasses, generatedClasses, appCDS.get());
        }

        if (!uberJarRequired.isEmpty() && !legacyJarRequired.isEmpty()) {
            throw new RuntimeException(
                    "Extensions with conflicting package types. One extension requires uber-jar another requires legacy format");
        }

        if (legacyJarRequired.isEmpty() && (!uberJarRequired.isEmpty()
                || packageConfig.type.equalsIgnoreCase(PackageConfig.BuiltInType.UBER_JAR.getValue()))) {
            return buildUberJar(curateOutcomeBuildItem, outputTargetBuildItem, transformedClasses, applicationArchivesBuildItem,
                    packageConfig, applicationInfo, generatedClasses, generatedResources, uberJarMergedResourceBuildItems,
                    uberJarIgnoredResourceBuildItems, mainClassBuildItem, classLoadingConfig);
        } else if (!legacyJarRequired.isEmpty() || packageConfig.isLegacyJar()) {
            return buildLegacyThinJar(curateOutcomeBuildItem, outputTargetBuildItem, transformedClasses,
                    applicationArchivesBuildItem,
                    packageConfig, applicationInfo, generatedClasses, generatedResources, mainClassBuildItem,
                    classLoadingConfig);
        } else {
            return buildThinJar(curateOutcomeBuildItem, outputTargetBuildItem, transformedClasses, applicationArchivesBuildItem,
                    packageConfig, classLoadingConfig, applicationInfo, generatedClasses, generatedResources,
                    additionalApplicationArchiveBuildItems, mainClassBuildItem);
        }
    }

    // the idea here is to just dump the class names of the generated and transformed classes into a file
    // that is read at runtime when AppCDS generation is requested
    private void handleAppCDSSupportFileGeneration(TransformedClassesBuildItem transformedClasses,
            List<GeneratedClassBuildItem> generatedClasses, AppCDSRequestedBuildItem appCDS) throws IOException {
        Path appCDsDir = appCDS.getAppCDSDir();
        Path generatedClassesFile = appCDsDir.resolve("generatedAndTransformed.lst");
        try (BufferedWriter writer = Files.newBufferedWriter(generatedClassesFile, StandardOpenOption.CREATE)) {
            StringBuilder classes = new StringBuilder();
            for (GeneratedClassBuildItem generatedClass : generatedClasses) {
                classes.append(generatedClass.getName().replace('/', '.')).append(System.lineSeparator());
            }

            for (Set<TransformedClassesBuildItem.TransformedClass> transformedClassesSet : transformedClasses
                    .getTransformedClassesByJar().values()) {
                for (TransformedClassesBuildItem.TransformedClass transformedClass : transformedClassesSet) {
                    if (transformedClass.getData() != null) {
                        classes.append(transformedClass.getFileName().replace('/', '.').replace(".class", ""))
                                .append(System.lineSeparator());
                    }
                }
            }

            if (classes.length() != 0) {
                writer.write(classes.toString());
            }
        }
    }

    private JarBuildItem buildUberJar(CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            PackageConfig packageConfig,
            ApplicationInfoBuildItem applicationInfo,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            List<UberJarMergedResourceBuildItem> mergeResources,
            List<UberJarIgnoredResourceBuildItem> ignoredResources,
            MainClassBuildItem mainClassBuildItem,
            ClassLoadingConfig classLoadingConfig) throws Exception {

        //we use the -runner jar name, unless we are building both types
        final Path runnerJar = outputTargetBuildItem.getOutputDirectory()
                .resolve(outputTargetBuildItem.getBaseName() + packageConfig.getRunnerSuffix() + DOT_JAR);

        // If the runner jar appears to exist already we create a new one with a tmp suffix.
        // Deleting an existing runner jar may result in deleting the original (non-runner) jar (in case the runner suffix is empty)
        // which is used as a source of content for the runner jar.
        final Path tmpRunnerJar;
        if (Files.exists(runnerJar)) {
            tmpRunnerJar = outputTargetBuildItem.getOutputDirectory()
                    .resolve(outputTargetBuildItem.getBaseName() + packageConfig.getRunnerSuffix() + ".tmp");
            Files.deleteIfExists(tmpRunnerJar);
        } else {
            tmpRunnerJar = runnerJar;
        }

        buildUberJar0(curateOutcomeBuildItem,
                outputTargetBuildItem,
                transformedClasses,
                applicationArchivesBuildItem,
                packageConfig,
                applicationInfo,
                generatedClasses,
                generatedResources,
                mergeResources,
                ignoredResources,
                mainClassBuildItem,
                classLoadingConfig,
                tmpRunnerJar);

        if (tmpRunnerJar != runnerJar) {
            Files.copy(tmpRunnerJar, runnerJar, StandardCopyOption.REPLACE_EXISTING);
            tmpRunnerJar.toFile().deleteOnExit();
        }

        //for uberjars we move the original jar, so there is only a single jar in the output directory
        final Path standardJar = outputTargetBuildItem.getOutputDirectory()
                .resolve(outputTargetBuildItem.getOriginalBaseName() + DOT_JAR);
        final Path originalJar = Files.exists(standardJar) ? standardJar : null;

        return new JarBuildItem(runnerJar, originalJar, null, PackageConfig.BuiltInType.UBER_JAR.getValue(),
                suffixToClassifier(packageConfig.getRunnerSuffix()));
    }

    private String suffixToClassifier(String suffix) {
        return suffix.startsWith("-") ? suffix.substring(1) : suffix;
    }

    private void buildUberJar0(CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            PackageConfig packageConfig,
            ApplicationInfoBuildItem applicationInfo,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            List<UberJarMergedResourceBuildItem> mergedResources,
            List<UberJarIgnoredResourceBuildItem> ignoredResources,
            MainClassBuildItem mainClassBuildItem,
            ClassLoadingConfig classLoadingConfig,
            Path runnerJar) throws Exception {
        try (FileSystem runnerZipFs = createNewZip(runnerJar, packageConfig)) {

            log.info("Building uber jar: " + runnerJar);

            final Map<String, String> seen = new HashMap<>();
            final Map<String, Set<Dependency>> duplicateCatcher = new HashMap<>();
            final Map<String, List<byte[]>> concatenatedEntries = new HashMap<>();
            final Set<String> mergeResourcePaths = mergedResources.stream()
                    .map(UberJarMergedResourceBuildItem::getPath)
                    .collect(Collectors.toSet());
            final Set<ArtifactKey> removed = getRemovedKeys(classLoadingConfig);

            Set<String> ignoredEntries = new HashSet<>();
            packageConfig.userConfiguredIgnoredEntries.ifPresent(ignoredEntries::addAll);
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

            final Collection<ResolvedDependency> appDeps = curateOutcomeBuildItem.getApplicationModel()
                    .getRuntimeDependencies();

            ResolvedDependency appArtifact = curateOutcomeBuildItem.getApplicationModel().getAppArtifact();
            // the manifest needs to be the first entry in the jar, otherwise JarInputStream does not work properly
            // see https://bugs.openjdk.java.net/browse/JDK-8031748
            generateManifest(runnerZipFs, "", packageConfig, appArtifact, mainClassBuildItem.getClassName(),
                    applicationInfo);

            for (ResolvedDependency appDep : appDeps) {

                // Exclude files that are not jars (typically, we can have XML files here, see https://github.com/quarkusio/quarkus/issues/2852)
                // and are not part of the optional dependencies to include
                if (!includeAppDep(appDep, outputTargetBuildItem.getIncludedOptionalDependencies(), removed)) {
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
                                walkFileDependencyForDependency(root, runnerZipFs, seen, duplicateCatcher, concatenatedEntries,
                                        allIgnoredEntriesPredicate, appDep, existingEntries, mergeResourcePaths);
                            }
                        }
                    } else {
                        walkFileDependencyForDependency(resolvedDep, runnerZipFs, seen, duplicateCatcher,
                                concatenatedEntries, allIgnoredEntriesPredicate, appDep, existingEntries,
                                mergeResourcePaths);
                    }
                }
            }
            Set<Set<Dependency>> explained = new HashSet<>();
            for (Map.Entry<String, Set<Dependency>> entry : duplicateCatcher.entrySet()) {
                if (entry.getValue().size() > 1) {
                    if (explained.add(entry.getValue())) {
                        log.warn("Dependencies with duplicate files detected. The dependencies " + entry.getValue()
                                + " contain duplicate files, e.g. " + entry.getKey());
                    }
                }
            }
            copyCommonContent(runnerZipFs, concatenatedEntries, applicationArchivesBuildItem, transformedClasses,
                    generatedClasses,
                    generatedResources, seen, allIgnoredEntriesPredicate);
            // now that all entries have been added, check if there's a META-INF/versions/ entry. If present,
            // mark this jar as multi-release jar. Strictly speaking, the jar spec expects META-INF/versions/N
            // directory where N is an integer greater than 8, but we don't do that level of checks here but that
            // should be OK.
            if (Files.isDirectory(runnerZipFs.getPath("META-INF", "versions"))) {
                log.debug("uber jar will be marked as multi-release jar");
                final Path manifestPath = runnerZipFs.getPath("META-INF", "MANIFEST.MF");
                final Manifest manifest = new Manifest();
                // read the existing one
                try (final InputStream is = Files.newInputStream(manifestPath)) {
                    manifest.read(is);
                }
                manifest.getMainAttributes().put(Attributes.Name.MULTI_RELEASE, "true");
                try (final OutputStream os = Files.newOutputStream(manifestPath)) {
                    manifest.write(os);
                }
            }
        }

        runnerJar.toFile().setReadable(true, false);
    }

    /**
     * Indicates whether the given dependency should be included or not.
     * <p>
     * A dependency should be included if it is a jar file and:
     * <p>
     * <ul>
     * <li>The dependency is not optional or</li>
     * <li>The dependency is part of the optional dependencies to include or</li>
     * <li>The optional dependencies to include are absent</li>
     * </ul>
     *
     * @param appDep the dependency to test.
     * @param optionalDependencies the optional dependencies to include into the final package.
     * @return {@code true} if the dependency should be included, {@code false} otherwise.
     */
    private static boolean includeAppDep(ResolvedDependency appDep, Optional<Set<ArtifactKey>> optionalDependencies,
            Set<ArtifactKey> removedArtifacts) {
        if (!appDep.isJar()) {
            return false;
        }
        if (appDep.isOptional()) {
            return optionalDependencies.map(appArtifactKeys -> appArtifactKeys.contains(appDep.getKey()))
                    .orElse(true);
        }
        if (removedArtifacts.contains(appDep.getKey())) {
            return false;
        }
        return true;
    }

    private void walkFileDependencyForDependency(Path root, FileSystem runnerZipFs, Map<String, String> seen,
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
                            addDir(runnerZipFs, relativePath);
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
                            if (log.isDebugEnabled()) {
                                log.debug("Signature file " + file.toAbsolutePath() + " from app " +
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
                                if (!seen.containsKey(relativePath)) {
                                    seen.put(relativePath, appDep.toString());
                                    Files.copy(file, runnerZipFs.getPath(relativePath),
                                            StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    @SuppressWarnings("deprecation")
    private JarBuildItem buildLegacyThinJar(CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            PackageConfig packageConfig,
            ApplicationInfoBuildItem applicationInfo,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            MainClassBuildItem mainClassBuildItem,
            ClassLoadingConfig classLoadingConfig) throws Exception {

        Path runnerJar = outputTargetBuildItem.getOutputDirectory()
                .resolve(outputTargetBuildItem.getBaseName() + packageConfig.getRunnerSuffix() + DOT_JAR);
        Path libDir = outputTargetBuildItem.getOutputDirectory().resolve("lib");
        Files.deleteIfExists(runnerJar);
        IoUtils.createOrEmptyDir(libDir);

        try (FileSystem runnerZipFs = createNewZip(runnerJar, packageConfig)) {

            log.info("Building thin jar: " + runnerJar);

            doLegacyThinJarGeneration(curateOutcomeBuildItem, outputTargetBuildItem, transformedClasses,
                    applicationArchivesBuildItem, applicationInfo,
                    packageConfig, generatedResources, libDir, generatedClasses, runnerZipFs, mainClassBuildItem,
                    classLoadingConfig);
        }
        runnerJar.toFile().setReadable(true, false);

        return new JarBuildItem(runnerJar, null, libDir, PackageConfig.BuiltInType.LEGACY_JAR.getValue(),
                suffixToClassifier(packageConfig.getRunnerSuffix()));
    }

    @SuppressWarnings("deprecation")
    private JarBuildItem buildThinJar(CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            PackageConfig packageConfig,
            ClassLoadingConfig classLoadingConfig,
            ApplicationInfoBuildItem applicationInfo,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            List<AdditionalApplicationArchiveBuildItem> additionalApplicationArchiveBuildItems,
            MainClassBuildItem mainClassBuildItem) throws Exception {

        boolean rebuild = outputTargetBuildItem.isRebuild();

        Path buildDir;

        if (packageConfig.outputDirectory.isPresent()) {
            buildDir = outputTargetBuildItem.getOutputDirectory();
        } else {
            buildDir = outputTargetBuildItem.getOutputDirectory().resolve(DEFAULT_FAST_JAR_DIRECTORY_NAME);
        }

        //unmodified 3rd party dependencies
        Path libDir = buildDir.resolve(LIB);
        Path mainLib = libDir.resolve(MAIN);
        //parent first entries
        Path baseLib = libDir.resolve(BOOT_LIB);
        Files.createDirectories(baseLib);

        Path appDir = buildDir.resolve(APP);
        Path quarkus = buildDir.resolve(QUARKUS);
        Path userProviders = null;
        if (packageConfig.userProvidersDirectory.isPresent()) {
            userProviders = buildDir.resolve(packageConfig.userProvidersDirectory.get());
        }
        if (!rebuild) {
            IoUtils.createOrEmptyDir(buildDir);
            Files.createDirectories(mainLib);
            Files.createDirectories(baseLib);
            Files.createDirectories(appDir);
            Files.createDirectories(quarkus);
            if (userProviders != null) {
                Files.createDirectories(userProviders);
                //we add this dir so that it can be copied into container images if required
                //and will still be copied even if empty
                Path keepFile = userProviders.resolve(".keep");
                if (!keepFile.toFile().exists()) {
                    // check if the file exists to avoid a FileAlreadyExistsException
                    Files.createFile(keepFile);
                }
            }
        } else {
            IoUtils.createOrEmptyDir(quarkus);
        }

        Path decompiledOutputDir = null;
        boolean wasDecompiledSuccessfully = true;
        Decompiler decompiler = null;
        if (packageConfig.vineflower.enabled.orElse(false) || packageConfig.decompiler.enabled.orElse(false)) {
            Optional<String> jarDirectoryStrOpt = packageConfig.vineflower.enabled.orElse(false)
                    ? packageConfig.vineflower.jarDirectory
                    : packageConfig.decompiler.jarDirectory;
            String jarDirectoryStr = jarDirectoryStrOpt.orElse(System.getProperty("user.home") + "/.quarkus");

            decompiledOutputDir = buildDir.getParent().resolve("decompiled");
            FileUtil.deleteDirectory(decompiledOutputDir);
            Files.createDirectory(decompiledOutputDir);
            decompiler = new Decompiler.VineflowerDecompiler();
            Path jarDirectory = Paths.get(jarDirectoryStr);
            if (!Files.exists(jarDirectory)) {
                Files.createDirectory(jarDirectory);
            }
            decompiler.init(new Decompiler.Context(VINEFLOWER_VERSION, jarDirectory, decompiledOutputDir));
            decompiler.downloadIfNecessary();
        }

        FastJarJars.FastJarJarsBuilder fastJarJarsBuilder = new FastJarJars.FastJarJarsBuilder();
        List<Path> parentFirst = new ArrayList<>();
        //we process in order of priority
        //transformed classes first
        if (!transformedClasses.getTransformedClassesByJar().isEmpty()) {
            Path transformedZip = quarkus.resolve(TRANSFORMED_BYTECODE_JAR);
            fastJarJarsBuilder.setTransformed(transformedZip);
            try (FileSystem out = createNewZip(transformedZip, packageConfig)) {
                for (Set<TransformedClassesBuildItem.TransformedClass> transformedSet : transformedClasses
                        .getTransformedClassesByJar().values()) {
                    for (TransformedClassesBuildItem.TransformedClass transformed : transformedSet) {
                        Path target = out.getPath(transformed.getFileName());
                        if (transformed.getData() != null) {
                            if (target.getParent() != null) {
                                Files.createDirectories(target.getParent());
                            }
                            Files.write(target, transformed.getData());
                        }
                    }
                }
            }
            if (decompiler != null) {
                wasDecompiledSuccessfully = decompiler.decompile(transformedZip);
            }
        }
        //now generated classes and resources
        Path generatedZip = quarkus.resolve(GENERATED_BYTECODE_JAR);
        fastJarJarsBuilder.setGenerated(generatedZip);
        try (FileSystem out = createNewZip(generatedZip, packageConfig)) {
            for (GeneratedClassBuildItem i : generatedClasses) {
                String fileName = i.getName().replace('.', '/') + ".class";
                Path target = out.getPath(fileName);
                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                Files.write(target, i.getClassData());
            }

            for (GeneratedResourceBuildItem i : generatedResources) {
                Path target = out.getPath(i.getName());
                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                Files.write(target, i.getData());
            }
        }
        if (decompiler != null) {
            wasDecompiledSuccessfully &= decompiler.decompile(generatedZip);
        }

        if (wasDecompiledSuccessfully && (decompiledOutputDir != null)) {
            log.info("The decompiled output can be found at: " + decompiledOutputDir.toAbsolutePath().toString());
        }

        //now the application classes
        Path runnerJar = appDir.resolve(outputTargetBuildItem.getBaseName() + DOT_JAR);
        fastJarJarsBuilder.setRunner(runnerJar);

        if (!rebuild) {
            Predicate<String> ignoredEntriesPredicate = getThinJarIgnoredEntriesPredicate(packageConfig);

            try (FileSystem runnerZipFs = createNewZip(runnerJar, packageConfig)) {
                copyFiles(applicationArchivesBuildItem.getRootArchive(), runnerZipFs, null, ignoredEntriesPredicate);
            }
        }
        final Set<ArtifactKey> parentFirstKeys = getParentFirstKeys(curateOutcomeBuildItem, classLoadingConfig);
        final StringBuilder classPath = new StringBuilder();
        final Set<ArtifactKey> removed = getRemovedKeys(classLoadingConfig);
        final Map<ArtifactKey, List<Path>> copiedArtifacts = new HashMap<>();
        for (ResolvedDependency appDep : curateOutcomeBuildItem.getApplicationModel().getRuntimeDependencies()) {
            if (!rebuild) {
                copyDependency(parentFirstKeys, outputTargetBuildItem, copiedArtifacts, mainLib, baseLib,
                        fastJarJarsBuilder::addDep, true,
                        classPath, appDep, transformedClasses, removed, packageConfig);
            } else if (includeAppDep(appDep, outputTargetBuildItem.getIncludedOptionalDependencies(), removed)) {
                appDep.getResolvedPaths().forEach(fastJarJarsBuilder::addDep);
            }
            if (parentFirstKeys.contains(appDep.getKey())) {
                appDep.getResolvedPaths().forEach(parentFirst::add);
            }
        }
        for (AdditionalApplicationArchiveBuildItem i : additionalApplicationArchiveBuildItems) {
            for (Path path : i.getResolvedPaths()) {
                if (!path.getParent().equals(userProviders)) {
                    throw new RuntimeException(
                            "Additional application archives can only be provided from the user providers directory. " + path
                                    + " is not present in " + userProviders);
                }
                fastJarJarsBuilder.addDep(path);
            }
        }

        /*
         * There are some files like META-INF/microprofile-config.properties that usually don't exist in application
         * and yet are always looked up (spec compliance...) and due to the location in the jar,
         * the RunnerClassLoader needs to look into every jar to determine whether they exist or not.
         * In keeping true to the original design of the RunnerClassLoader which indexes the directory structure,
         * we just add a fail-fast path for files we know don't exist.
         *
         * TODO: if this gets more complex, we'll probably want a build item to carry this information instead of hard
         * coding it here
         */
        List<String> nonExistentResources = new ArrayList<>(1);
        Enumeration<URL> mpConfigURLs = Thread.currentThread().getContextClassLoader().getResources(MP_CONFIG_FILE);
        if (!mpConfigURLs.hasMoreElements()) {
            nonExistentResources.add(MP_CONFIG_FILE);
        }

        Path appInfo = buildDir.resolve(QuarkusEntryPoint.QUARKUS_APPLICATION_DAT);
        try (OutputStream out = Files.newOutputStream(appInfo)) {
            FastJarJars fastJarJars = fastJarJarsBuilder.build();
            List<Path> allJars = new ArrayList<>();
            if (fastJarJars.transformed != null) {
                allJars.add(fastJarJars.transformed);
            }
            allJars.add(fastJarJars.generated);
            allJars.add(fastJarJars.runner);
            List<Path> sortedDeps = new ArrayList<>(fastJarJars.deps);
            Collections.sort(sortedDeps);
            allJars.addAll(sortedDeps);
            List<Path> sortedParentFirst = new ArrayList<>(parentFirst);
            Collections.sort(sortedParentFirst);
            List<String> sortedNonExistentResources = new ArrayList<>(nonExistentResources);
            Collections.sort(sortedNonExistentResources);
            SerializedApplication.write(out, mainClassBuildItem.getClassName(), buildDir, allJars, sortedParentFirst,
                    sortedNonExistentResources);
        }

        runnerJar.toFile().setReadable(true, false);
        Path initJar = buildDir.resolve(QUARKUS_RUN_JAR);
        boolean mutableJar = packageConfig.type.equalsIgnoreCase(PackageConfig.BuiltInType.MUTABLE_JAR.getValue());
        if (mutableJar) {
            //we output the properties in a reproducible manner, so we remove the date comment
            //and sort them
            //we still use Properties to get the escaping right though, so basically we write out the lines
            //to memory, split them, discard comments, sort them, then write them to disk
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            outputTargetBuildItem.getBuildSystemProperties().store(out, null);
            List<String> lines = Arrays.stream(out.toString(StandardCharsets.UTF_8).split("\n"))
                    .filter(s -> !s.startsWith("#")).sorted().collect(Collectors.toList());
            Path buildSystemProps = quarkus.resolve(BUILD_SYSTEM_PROPERTIES);
            try (OutputStream fileOutput = Files.newOutputStream(buildSystemProps)) {
                fileOutput.write(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
            }
        }
        if (!rebuild) {
            try (FileSystem runnerZipFs = createNewZip(initJar, packageConfig)) {
                ResolvedDependency appArtifact = curateOutcomeBuildItem.getApplicationModel().getAppArtifact();
                generateManifest(runnerZipFs, classPath.toString(), packageConfig, appArtifact,
                        QuarkusEntryPoint.class.getName(),
                        applicationInfo);
            }

            //now copy the deployment artifacts, if required
            if (mutableJar) {
                Path deploymentLib = libDir.resolve(DEPLOYMENT_LIB);
                Files.createDirectories(deploymentLib);
                for (ResolvedDependency appDep : curateOutcomeBuildItem.getApplicationModel().getDependencies()) {
                    copyDependency(parentFirstKeys, outputTargetBuildItem, copiedArtifacts, deploymentLib, baseLib, (p) -> {
                    },
                            false, classPath,
                            appDep, new TransformedClassesBuildItem(Map.of()), removed, packageConfig); //we don't care about transformation here, so just pass in an empty item
                }
                Map<ArtifactKey, List<String>> relativePaths = new HashMap<>();
                for (Map.Entry<ArtifactKey, List<Path>> e : copiedArtifacts.entrySet()) {
                    relativePaths.put(e.getKey(),
                            e.getValue().stream().map(s -> buildDir.relativize(s).toString().replace('\\', '/'))
                                    .collect(Collectors.toList()));
                }

                //now we serialize the data needed to build up the reaugmentation class path
                //first the app model
                MutableJarApplicationModel model = new MutableJarApplicationModel(outputTargetBuildItem.getBaseName(),
                        relativePaths,
                        curateOutcomeBuildItem.getApplicationModel(),
                        packageConfig.userProvidersDirectory.orElse(null), buildDir.relativize(runnerJar).toString());
                Path appmodelDat = deploymentLib.resolve(APPMODEL_DAT);
                try (OutputStream out = Files.newOutputStream(appmodelDat)) {
                    ObjectOutputStream obj = new ObjectOutputStream(out);
                    obj.writeObject(model);
                    obj.close();
                }
                //now the bootstrap CP
                //we just include all deployment deps, even though we only really need bootstrap
                //as we don't really have a resolved bootstrap CP
                //once we have the app model it will all be done in QuarkusClassLoader anyway
                Path deploymentCp = deploymentLib.resolve(DEPLOYMENT_CLASS_PATH_DAT);
                try (OutputStream out = Files.newOutputStream(deploymentCp)) {
                    ObjectOutputStream obj = new ObjectOutputStream(out);
                    List<String> paths = new ArrayList<>();
                    for (ResolvedDependency i : curateOutcomeBuildItem.getApplicationModel().getDependencies()) {
                        final List<String> list = relativePaths.get(i.getKey());
                        // some of the dependencies may have been filtered out
                        if (list != null) {
                            paths.addAll(list);
                        }
                    }
                    obj.writeObject(paths);
                    obj.close();
                }
            }

            if (packageConfig.includeDependencyList) {
                Path deplist = buildDir.resolve(QUARKUS_APP_DEPS);
                List<String> lines = new ArrayList<>();
                for (ResolvedDependency i : curateOutcomeBuildItem.getApplicationModel().getRuntimeDependencies()) {
                    lines.add(i.toGACTVString());
                }
                lines.sort(Comparator.naturalOrder());
                Files.write(deplist, lines);
            }
        } else {
            //if it is a rebuild we might have classes
        }
        try (Stream<Path> files = Files.walk(buildDir)) {
            files.forEach(new Consumer<Path>() {
                @Override
                public void accept(Path path) {
                    path.toFile().setReadable(true, false);
                }
            });
        }
        return new JarBuildItem(initJar, null, libDir, packageConfig.type, null);
    }

    /**
     * @return a {@code Set} containing the key of the artifacts to load from the parent ClassLoader first.
     */
    private Set<ArtifactKey> getParentFirstKeys(CurateOutcomeBuildItem curateOutcomeBuildItem,
            ClassLoadingConfig classLoadingConfig) {
        final Set<ArtifactKey> parentFirstKeys = new HashSet<>();
        curateOutcomeBuildItem.getApplicationModel().getDependencies().forEach(d -> {
            if (d.isFlagSet(DependencyFlags.CLASSLOADER_RUNNER_PARENT_FIRST)) {
                parentFirstKeys.add(d.getKey());
            }
        });
        classLoadingConfig.parentFirstArtifacts.ifPresent(
                parentFirstArtifacts -> {
                    for (String artifact : parentFirstArtifacts) {
                        parentFirstKeys.add(new GACT(artifact.split(":")));
                    }
                });
        return parentFirstKeys;
    }

    /**
     * @return a {@code Set} containing the key of the artifacts to load from the parent ClassLoader first.
     */
    private Set<ArtifactKey> getRemovedKeys(ClassLoadingConfig classLoadingConfig) {
        final Set<ArtifactKey> removed = new HashSet<>();
        classLoadingConfig.removedArtifacts.ifPresent(
                removedArtifacts -> {
                    for (String artifact : removedArtifacts) {
                        removed.add(new GACT(artifact.split(":")));
                    }
                });
        return removed;
    }

    private void copyDependency(Set<ArtifactKey> parentFirstArtifacts, OutputTargetBuildItem outputTargetBuildItem,
            Map<ArtifactKey, List<Path>> runtimeArtifacts, Path libDir, Path baseLib, Consumer<Path> targetPathConsumer,
            boolean allowParentFirst, StringBuilder classPath, ResolvedDependency appDep,
            TransformedClassesBuildItem transformedClasses, Set<ArtifactKey> removedDeps,
            PackageConfig packageConfig)
            throws IOException {

        // Exclude files that are not jars (typically, we can have XML files here, see https://github.com/quarkusio/quarkus/issues/2852)
        // and are not part of the optional dependencies to include
        if (!includeAppDep(appDep, outputTargetBuildItem.getIncludedOptionalDependencies(), removedDeps)) {
            return;
        }
        if (runtimeArtifacts.containsKey(appDep.getKey())) {
            return;
        }
        for (Path resolvedDep : appDep.getResolvedPaths()) {
            final String fileName = appDep.getGroupId() + "." + resolvedDep.getFileName();
            final Path targetPath;

            if (allowParentFirst && parentFirstArtifacts.contains(appDep.getKey())) {
                targetPath = baseLib.resolve(fileName);
                classPath.append(" ").append(LIB).append("/").append(BOOT_LIB).append("/").append(fileName);
            } else {
                targetPath = libDir.resolve(fileName);
                targetPathConsumer.accept(targetPath);
            }
            runtimeArtifacts.computeIfAbsent(appDep.getKey(), (s) -> new ArrayList<>(1)).add(targetPath);

            if (Files.isDirectory(resolvedDep)) {
                // This case can happen when we are building a jar from inside the Quarkus repository
                // and Quarkus Bootstrap's localProjectDiscovery has been set to true. In such a case
                // the non-jar dependencies are the Quarkus dependencies picked up on the file system
                packageClasses(resolvedDep, targetPath, packageConfig);
            } else {
                Set<TransformedClassesBuildItem.TransformedClass> transformedFromThisArchive = transformedClasses
                        .getTransformedClassesByJar().get(resolvedDep);
                Set<String> removedFromThisArchive = new HashSet<>();
                if (transformedFromThisArchive != null) {
                    for (TransformedClassesBuildItem.TransformedClass i : transformedFromThisArchive) {
                        if (i.getData() == null) {
                            removedFromThisArchive.add(i.getFileName());
                        }
                    }
                }
                if (removedFromThisArchive.isEmpty()) {
                    Files.copy(resolvedDep, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    //we have removed classes, we need to handle them correctly
                    filterZipFile(resolvedDep, targetPath, removedFromThisArchive);
                }
            }
        }
    }

    private void packageClasses(Path resolvedDep, final Path targetPath, PackageConfig packageConfig) throws IOException {
        try (FileSystem runnerZipFs = createNewZip(targetPath, packageConfig)) {
            Files.walkFileTree(resolvedDep, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                throws IOException {
                            final Path relativePath = resolvedDep.relativize(file);
                            final Path targetPath = runnerZipFs.getPath(relativePath.toString());
                            if (targetPath.getParent() != null) {
                                Files.createDirectories(targetPath.getParent());
                            }
                            Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING); //replace only needed for testing
                            return FileVisitResult.CONTINUE;
                        }
                    });
        }
    }

    /**
     * Native images are built from a specially created jar file. This allows for changes in how the jar file is generated.
     */
    @BuildStep
    public NativeImageSourceJarBuildItem buildNativeImageJar(CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedNativeImageClassBuildItem> nativeImageResources,
            List<GeneratedResourceBuildItem> generatedResources,
            MainClassBuildItem mainClassBuildItem,
            ClassLoadingConfig classLoadingConfig) throws Exception {
        Path targetDirectory = outputTargetBuildItem.getOutputDirectory()
                .resolve(outputTargetBuildItem.getBaseName() + "-native-image-source-jar");
        IoUtils.createOrEmptyDir(targetDirectory);

        List<GeneratedClassBuildItem> allClasses = new ArrayList<>(generatedClasses);
        allClasses.addAll(nativeImageResources.stream()
                .map((s) -> new GeneratedClassBuildItem(true, s.getName(), s.getClassData()))
                .collect(Collectors.toList()));

        return buildNativeImageThinJar(curateOutcomeBuildItem, outputTargetBuildItem, transformedClasses,
                applicationArchivesBuildItem,
                applicationInfo, packageConfig, allClasses, generatedResources, mainClassBuildItem, targetDirectory,
                classLoadingConfig);
    }

    private NativeImageSourceJarBuildItem buildNativeImageThinJar(CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            List<GeneratedClassBuildItem> allClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            MainClassBuildItem mainClassBuildItem,
            Path targetDirectory,
            ClassLoadingConfig classLoadingConfig) throws Exception {
        copyJsonConfigFiles(applicationArchivesBuildItem, targetDirectory);

        Path runnerJar = targetDirectory
                .resolve(outputTargetBuildItem.getBaseName() + packageConfig.getRunnerSuffix() + DOT_JAR);
        Path libDir = targetDirectory.resolve(LIB);
        Files.createDirectories(libDir);

        try (FileSystem runnerZipFs = ZipUtils.newZip(runnerJar)) {

            log.info("Building native image source jar: " + runnerJar);

            // Remove svm and graal-sdk artifacts as they are provided by GraalVM itself
            if (classLoadingConfig.removedArtifacts.isEmpty()) {
                classLoadingConfig.removedArtifacts = Optional.of(new ArrayList<>(6));
            }
            List<String> removedArtifacts = classLoadingConfig.removedArtifacts.get();
            removedArtifacts.add("org.graalvm.nativeimage:svm");
            removedArtifacts.add("org.graalvm.sdk:graal-sdk");
            removedArtifacts.add("org.graalvm.sdk:nativeimage");
            removedArtifacts.add("org.graalvm.sdk:word");
            removedArtifacts.add("org.graalvm.sdk:collections");

            doLegacyThinJarGeneration(curateOutcomeBuildItem, outputTargetBuildItem, transformedClasses,
                    applicationArchivesBuildItem, applicationInfo, packageConfig, generatedResources, libDir, allClasses,
                    runnerZipFs, mainClassBuildItem, classLoadingConfig);
        }
        runnerJar.toFile().setReadable(true, false);
        return new NativeImageSourceJarBuildItem(runnerJar, libDir);
    }

    /**
     * This is done in order to make application specific native image configuration files available to the native-image tool
     * without the user needing to know any specific paths.
     * The files that are copied don't end up in the native image unless the user specifies they are needed, all this method
     * does is copy them to a convenient location
     */
    private void copyJsonConfigFiles(ApplicationArchivesBuildItem applicationArchivesBuildItem, Path thinJarDirectory)
            throws IOException {
        for (Path root : applicationArchivesBuildItem.getRootArchive().getRootDirectories()) {
            try (Stream<Path> stream = Files.find(root, 1, IS_JSON_FILE_PREDICATE)) {
                stream.forEach(new Consumer<Path>() {
                    @Override
                    public void accept(Path jsonPath) {
                        try {
                            Files.createDirectories(thinJarDirectory);
                            Files.copy(jsonPath, thinJarDirectory.resolve(jsonPath.getFileName().toString()));
                        } catch (IOException e) {
                            throw new UncheckedIOException(
                                    "Unable to copy json config file from " + jsonPath + " to " + thinJarDirectory,
                                    e);
                        }
                    }
                });
            }
        }
    }

    private void doLegacyThinJarGeneration(CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            List<GeneratedResourceBuildItem> generatedResources,
            Path libDir,
            List<GeneratedClassBuildItem> allClasses,
            FileSystem runnerZipFs,
            MainClassBuildItem mainClassBuildItem,
            ClassLoadingConfig classLoadingConfig)
            throws IOException {
        final Map<String, String> seen = new HashMap<>();
        final StringBuilder classPath = new StringBuilder();
        final Map<String, List<byte[]>> services = new HashMap<>();

        final Collection<ResolvedDependency> appDeps = curateOutcomeBuildItem.getApplicationModel()
                .getRuntimeDependencies();

        Predicate<String> ignoredEntriesPredicate = getThinJarIgnoredEntriesPredicate(packageConfig);

        final Set<ArtifactKey> removed = getRemovedKeys(classLoadingConfig);
        copyLibraryJars(runnerZipFs, outputTargetBuildItem, transformedClasses, libDir, classPath, appDeps, services,
                ignoredEntriesPredicate, removed);

        ResolvedDependency appArtifact = curateOutcomeBuildItem.getApplicationModel().getAppArtifact();
        // the manifest needs to be the first entry in the jar, otherwise JarInputStream does not work properly
        // see https://bugs.openjdk.java.net/browse/JDK-8031748
        generateManifest(runnerZipFs, classPath.toString(), packageConfig, appArtifact, mainClassBuildItem.getClassName(),
                applicationInfo);

        copyCommonContent(runnerZipFs, services, applicationArchivesBuildItem, transformedClasses, allClasses,
                generatedResources, seen, ignoredEntriesPredicate);
    }

    private void copyLibraryJars(FileSystem runnerZipFs, OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses, Path libDir,
            StringBuilder classPath, Collection<ResolvedDependency> appDeps, Map<String, List<byte[]>> services,
            Predicate<String> ignoredEntriesPredicate, Set<ArtifactKey> removedDependencies) throws IOException {

        for (ResolvedDependency appDep : appDeps) {

            // Exclude files that are not jars (typically, we can have XML files here, see https://github.com/quarkusio/quarkus/issues/2852)
            // and are not part of the optional dependencies to include
            if (!includeAppDep(appDep, outputTargetBuildItem.getIncludedOptionalDependencies(), removedDependencies)) {
                continue;
            }

            for (Path resolvedDep : appDep.getResolvedPaths()) {
                if (!Files.isDirectory(resolvedDep)) {
                    Set<String> transformedFromThisArchive = transformedClasses.getTransformedFilesByJar().get(resolvedDep);
                    if (transformedFromThisArchive == null || transformedFromThisArchive.isEmpty()) {
                        final String fileName = appDep.getGroupId() + "." + resolvedDep.getFileName();
                        final Path targetPath = libDir.resolve(fileName);
                        Files.copy(resolvedDep, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        classPath.append(" lib/").append(fileName);
                    } else {
                        //we have transformed classes, we need to handle them correctly
                        final String fileName = "modified-" + appDep.getGroupId() + "."
                                + resolvedDep.getFileName();
                        final Path targetPath = libDir.resolve(fileName);
                        classPath.append(" lib/").append(fileName);
                        filterZipFile(resolvedDep, targetPath, transformedFromThisArchive);
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

    private void copyCommonContent(FileSystem runnerZipFs, Map<String, List<byte[]>> concatenatedEntries,
            ApplicationArchivesBuildItem appArchives, TransformedClassesBuildItem transformedClassesBuildItem,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources, Map<String, String> seen,
            Predicate<String> ignoredEntriesPredicate)
            throws IOException {

        //TODO: this is probably broken in gradle
        //        if (Files.exists(augmentOutcome.getConfigDir())) {
        //            copyFiles(augmentOutcome.getConfigDir(), runnerZipFs, services);
        //        }
        for (Set<TransformedClassesBuildItem.TransformedClass> transformed : transformedClassesBuildItem
                .getTransformedClassesByJar().values()) {
            for (TransformedClassesBuildItem.TransformedClass i : transformed) {
                if (i.getData() != null) {
                    Path target = runnerZipFs.getPath(i.getFileName());
                    handleParent(runnerZipFs, i.getFileName(), seen);
                    try (final OutputStream out = wrapForJDK8232879(Files.newOutputStream(target))) {
                        out.write(i.getData());
                    }
                    seen.put(i.getFileName(), "Current Application");
                }
            }
        }
        for (GeneratedClassBuildItem i : generatedClasses) {
            String fileName = i.getName().replace('.', '/') + ".class";
            seen.put(fileName, "Current Application");
            Path target = runnerZipFs.getPath(fileName);
            handleParent(runnerZipFs, fileName, seen);
            if (Files.exists(target)) {
                continue;
            }
            try (final OutputStream os = wrapForJDK8232879(Files.newOutputStream(target))) {
                os.write(i.getClassData());
            }
        }

        for (GeneratedResourceBuildItem i : generatedResources) {
            if (ignoredEntriesPredicate.test(i.getName())) {
                continue;
            }
            Path target = runnerZipFs.getPath(i.getName());
            handleParent(runnerZipFs, i.getName(), seen);
            if (Files.exists(target)) {
                continue;
            }
            if (i.getName().startsWith("META-INF/services/")) {
                concatenatedEntries.computeIfAbsent(i.getName(), (u) -> new ArrayList<>()).add(i.getData());
            } else {
                try (final OutputStream os = wrapForJDK8232879(Files.newOutputStream(target))) {
                    os.write(i.getData());
                }
            }
        }

        copyFiles(appArchives.getRootArchive(), runnerZipFs, concatenatedEntries, ignoredEntriesPredicate);

        for (Map.Entry<String, List<byte[]>> entry : concatenatedEntries.entrySet()) {
            try (final OutputStream os = wrapForJDK8232879(
                    Files.newOutputStream(runnerZipFs.getPath(entry.getKey())))) {
                // TODO: Handle merging of XMLs
                for (byte[] i : entry.getValue()) {
                    os.write(i);
                    os.write('\n');
                }
            }
        }
    }

    private void handleParent(FileSystem runnerZipFs, String fileName, Map<String, String> seen) throws IOException {
        for (int i = 0; i < fileName.length(); ++i) {
            if (fileName.charAt(i) == '/') {
                String dir = fileName.substring(0, i);
                if (!seen.containsKey(dir)) {
                    seen.put(dir, "Current Application");
                    Files.createDirectories(runnerZipFs.getPath(dir));
                }
            }
        }
    }

    private void filterZipFile(Path resolvedDep, Path targetPath, Set<String> transformedFromThisArchive) {

        try {
            byte[] buffer = new byte[10000];
            try (ZipFile in = new ZipFile(resolvedDep.toFile())) {
                try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(targetPath.toFile()))) {
                    Enumeration<? extends ZipEntry> entries = in.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (!transformedFromThisArchive.contains(entry.getName())) {
                            entry.setCompressedSize(-1);
                            out.putNextEntry(entry);
                            try (InputStream inStream = in.getInputStream(entry)) {
                                int r = 0;
                                while ((r = inStream.read(buffer)) > 0) {
                                    out.write(buffer, 0, r);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Manifest generation is quite simple : we just have to push some attributes in manifest.
     * However, it gets a little more complex if the manifest preexists.
     * So we first try to see if a manifest exists, and otherwise create a new one.
     *
     * <b>BEWARE</b> this method should be invoked after file copy from target/classes and so on.
     * Otherwise, this manifest manipulation will be useless.
     */
    private void generateManifest(FileSystem runnerZipFs, final String classPath, PackageConfig config,
            ResolvedDependency appArtifact,
            String mainClassName,
            ApplicationInfoBuildItem applicationInfo)
            throws IOException {
        final Path manifestPath = runnerZipFs.getPath("META-INF", "MANIFEST.MF");
        final Manifest manifest = new Manifest();
        if (Files.exists(manifestPath)) {
            try (InputStream is = Files.newInputStream(manifestPath)) {
                manifest.read(is);
            }
            Files.delete(manifestPath);
        } else {
            Files.createDirectories(runnerZipFs.getPath("META-INF"));
        }
        Files.createDirectories(manifestPath.getParent());
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        if (config.manifest.attributes.size() > 0) {
            for (Map.Entry<String, String> attribute : config.manifest.attributes.entrySet()) {
                attributes.putValue(attribute.getKey(), attribute.getValue());
            }
        }
        if (attributes.containsKey(Attributes.Name.CLASS_PATH)) {
            log.warn(
                    "A CLASS_PATH entry was already defined in your MANIFEST.MF or using the property quarkus.package.manifest.attributes.\"Class-Path\". Quarkus has overwritten this existing entry.");
        }
        attributes.put(Attributes.Name.CLASS_PATH, classPath);
        if (attributes.containsKey(Attributes.Name.MAIN_CLASS)) {
            String existingMainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
            if (!mainClassName.equals(existingMainClass)) {
                log.warn(
                        "A MAIN_CLASS entry was already defined in your MANIFEST.MF or using the property quarkus.package.manifest.attributes.\"Main-Class\". Quarkus has overwritten your existing entry.");
            }
        }
        attributes.put(Attributes.Name.MAIN_CLASS, mainClassName);
        if (config.manifest.addImplementationEntries && !attributes.containsKey(Attributes.Name.IMPLEMENTATION_TITLE)) {
            String name = ApplicationInfoBuildItem.UNSET_VALUE.equals(applicationInfo.getName())
                    ? appArtifact.getArtifactId()
                    : applicationInfo.getName();
            attributes.put(Attributes.Name.IMPLEMENTATION_TITLE, name);
        }
        if (config.manifest.addImplementationEntries && !attributes.containsKey(Attributes.Name.IMPLEMENTATION_VERSION)) {
            String version = ApplicationInfoBuildItem.UNSET_VALUE.equals(applicationInfo.getVersion())
                    ? appArtifact.getVersion()
                    : applicationInfo.getVersion();
            attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, version);
        }
        if (config.manifest.manifestSections.size() > 0) {
            for (String sectionName : config.manifest.manifestSections.keySet()) {
                for (Map.Entry<String, String> entry : config.manifest.manifestSections.get(sectionName).entrySet()) {
                    Attributes attribs = manifest.getEntries().computeIfAbsent(sectionName, k -> new Attributes());
                    attribs.putValue(entry.getKey(), entry.getValue());
                }
            }
        }
        try (final OutputStream os = wrapForJDK8232879(Files.newOutputStream(manifestPath))) {
            manifest.write(os);
        }
    }

    /**
     * Copy files from {@code archive} to {@code fs}, filtering out service providers into the given map.
     *
     * @param archive the root application archive
     * @param fs the destination filesystem
     * @param services the services map
     * @throws IOException if an error occurs
     */
    private void copyFiles(ApplicationArchive archive, FileSystem fs, Map<String, List<byte[]>> services,
            Predicate<String> ignoredEntriesPredicate) throws IOException {
        try {
            archive.accept(tree -> {
                tree.walk(new PathVisitor() {
                    @Override
                    public void visitPath(PathVisit visit) {
                        final Path file = visit.getRoot().relativize(visit.getPath());
                        final String relativePath = toUri(file);
                        if (relativePath.isEmpty() || ignoredEntriesPredicate.test(relativePath)) {
                            return;
                        }
                        try {
                            if (Files.isDirectory(visit.getPath())) {
                                addDir(fs, relativePath);
                            } else {
                                if (relativePath.startsWith("META-INF/services/") && relativePath.length() > 18
                                        && services != null) {
                                    final byte[] content;
                                    try {
                                        content = Files.readAllBytes(visit.getPath());
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                    services.computeIfAbsent(relativePath, (u) -> new ArrayList<>()).add(content);
                                } else if (!relativePath.equals("META-INF/INDEX.LIST")) {
                                    //TODO: auto generate INDEX.LIST
                                    //this may have implications for Camel though, as they change the layout
                                    //also this is only really relevant for the thin jar layout
                                    Path target = fs.getPath(relativePath);
                                    if (!Files.exists(target)) {
                                        Files.copy(visit.getPath(), target, StandardCopyOption.REPLACE_EXISTING);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                });
            });
        } catch (RuntimeException re) {
            final Throwable cause = re.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw re;
        }

    }

    private void addDir(FileSystem fs, final String relativePath)
            throws IOException {
        final Path targetDir = fs.getPath(relativePath);
        try {
            Files.createDirectory(targetDir);
        } catch (FileAlreadyExistsException e) {
            if (!Files.isDirectory(targetDir)) {
                throw e;
            }
        }
    }

    private static String toUri(Path path) {
        if (path.isAbsolute()) {
            return path.toUri().getPath();
        }
        if (path.getNameCount() == 0) {
            return "";
        }
        return toUri(new StringBuilder(), path, 0).toString();
    }

    private static StringBuilder toUri(StringBuilder b, Path path, int seg) {
        b.append(path.getName(seg));
        if (seg < path.getNameCount() - 1) {
            b.append('/');
            toUri(b, path, seg + 1);
        }
        return b;
    }

    static class JarRequired implements BooleanSupplier {

        private final PackageConfig packageConfig;

        JarRequired(PackageConfig packageConfig) {
            this.packageConfig = packageConfig;
        }

        @Override
        public boolean getAsBoolean() {
            return packageConfig.isAnyJarType();
        }
    }

    // same as the impl in sun.security.util.SignatureFileVerifier#isBlockOrSF()
    static boolean isBlockOrSF(final String s) {
        if (s == null) {
            return false;
        }
        return s.endsWith(".SF")
                || s.endsWith(".DSA")
                || s.endsWith(".RSA")
                || s.endsWith(".EC");
    }

    private Predicate<String> getThinJarIgnoredEntriesPredicate(PackageConfig packageConfig) {
        if (packageConfig.userConfiguredIgnoredEntries.isEmpty()) {
            return new Predicate<String>() {
                @Override
                public boolean test(String path) {
                    return false;
                }
            };
        }

        Set<String> ignoredEntries = new HashSet<>(packageConfig.userConfiguredIgnoredEntries.get());
        Predicate<String> ignoredEntriesPredicate = new Predicate<String>() {
            @Override
            public boolean test(String path) {
                return ignoredEntries.contains(path);
            }
        };
        return ignoredEntriesPredicate;
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
                "LICENSE");

        @Override
        public boolean test(String path) {
            return UBER_JAR_IGNORED_ENTRIES.contains(path)
                    || path.endsWith("module-info.class");
        }
    }

    static class FastJarJars {
        private final Path transformed;
        private final Path generated;
        private final Path runner;
        private final List<Path> deps;

        public FastJarJars(FastJarJarsBuilder builder) {
            this.transformed = builder.transformed;
            this.generated = builder.generated;
            this.runner = builder.runner;
            this.deps = builder.deps;
        }

        public static class FastJarJarsBuilder {
            private Path transformed;
            private Path generated;
            private Path runner;
            private final List<Path> deps = new ArrayList<>();

            public FastJarJarsBuilder setTransformed(Path transformed) {
                this.transformed = transformed;
                return this;
            }

            public FastJarJarsBuilder setGenerated(Path generated) {
                this.generated = generated;
                return this;
            }

            public FastJarJarsBuilder setRunner(Path runner) {
                this.runner = runner;
                return this;
            }

            public FastJarJarsBuilder addDep(Path dep) {
                this.deps.add(dep);
                return this;
            }

            public FastJarJars build() {
                return new FastJarJars(this);
            }
        }
    }

    private static class IsJsonFilePredicate implements BiPredicate<Path, BasicFileAttributes> {

        @Override
        public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
            return basicFileAttributes.isRegularFile() && path.toString().endsWith(".json");
        }
    }

    private interface Decompiler {

        void init(Context context);

        /**
         * @return {@code true} if the decompiler was successfully download or already exists
         */
        boolean downloadIfNecessary();

        /**
         * @return {@code true} if the decompilation process was successful
         */
        boolean decompile(Path jarToDecompile);

        class Context {
            final String versionStr;
            final Path jarLocation;
            final Path decompiledOutputDir;

            public Context(String versionStr, Path jarLocation, Path decompiledOutputDir) {
                this.versionStr = versionStr;
                this.jarLocation = jarLocation;
                this.decompiledOutputDir = decompiledOutputDir;
            }

        }

        class VineflowerDecompiler implements Decompiler {

            private Context context;
            private Path decompilerJar;

            @Override
            public void init(Context context) {
                this.context = context;
                this.decompilerJar = context.jarLocation.resolve(String.format("vineflower-%s.jar", context.versionStr));
            }

            @Override
            public boolean downloadIfNecessary() {
                if (Files.exists(decompilerJar)) {
                    return true;
                }
                String downloadURL = String.format(
                        "https://repo.maven.apache.org/maven2/org/vineflower/vineflower/%s/vineflower-%s.jar",
                        context.versionStr, context.versionStr);
                try (BufferedInputStream in = new BufferedInputStream(new URL(downloadURL).openStream());
                        FileOutputStream fileOutputStream = new FileOutputStream(decompilerJar.toFile())) {
                    byte[] dataBuffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }
                    return true;
                } catch (IOException e) {
                    log.error("Unable to download Vineflower from " + downloadURL, e);
                    return false;
                }
            }

            @Override
            public boolean decompile(Path jarToDecompile) {
                int exitCode;
                try {
                    int dotIndex = jarToDecompile.getFileName().toString().indexOf('.');
                    String fileName = jarToDecompile.getFileName().toString().substring(0, dotIndex);
                    ProcessBuilder processBuilder = new ProcessBuilder(
                            Arrays.asList(
                                    JavaBinFinder.findBin(),
                                    "-jar",
                                    decompilerJar.toAbsolutePath().toString(),
                                    "-rsy=0", // synthetic methods
                                    "-rbr=0", // bridge methods
                                    jarToDecompile.toAbsolutePath().toString(),
                                    context.decompiledOutputDir.resolve(fileName).toAbsolutePath().toString()));
                    if (log.isDebugEnabled()) {
                        processBuilder.inheritIO();
                    } else {
                        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD.file())
                                .redirectOutput(ProcessBuilder.Redirect.DISCARD.file());
                    }
                    exitCode = processBuilder.start().waitFor();
                } catch (Exception e) {
                    log.error("Failed to launch decompiler.", e);
                    return false;
                }

                if (exitCode != 0) {
                    log.errorf("Vineflower decompiler exited with error code: %d.", exitCode);
                    return false;
                }

                return true;
            }
        }
    }

    private static FileSystem createNewZip(Path runnerJar, PackageConfig config) throws IOException {
        boolean useUncompressedJar = config.compressJar.map(o -> !o).orElse(false);
        if (useUncompressedJar) {
            return ZipUtils.newZip(runnerJar, Map.of("compressionMethod", "STORED"));
        }
        return ZipUtils.newZip(runnerJar);
    }

}
