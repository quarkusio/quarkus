package io.quarkus.deployment.pkg.steps;

import static io.quarkus.bootstrap.util.ZipUtils.wrapForJDK8232879;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarRequiredBuildItem;

/**
 * This build step builds both the thin jars and uber jars.
 *
 * The way this is built is a bit convoluted. In general we only want a single one built,
 * as determined by the {@link PackageConfig} (unless the config explicitly asks for both of them)
 *
 * However we still need an extension to be able to ask for a specify one of these despite the config,
 * e.g. if a serverless environment needs an uberjar to build its deployment package then we need
 * to be able to provide this.
 *
 * To enable this we have two build steps that strongly produce the respective artifact type build
 * items, but not a {@link ArtifactResultBuildItem}. We then
 * have another two build steps that only run if they are configured too that consume these explicit
 * build items and transform them into {@link ArtifactResultBuildItem}.
 */
public class JarResultBuildStep {

    private static final Set<String> IGNORED_ENTRIES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "META-INF/INDEX.LIST",
            "META-INF/MANIFEST.MF",
            "module-info.class",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/LICENSE.md",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt",
            "META-INF/NOTICE.md",
            "META-INF/README",
            "META-INF/README.txt",
            "META-INF/README.md",
            "META-INF/DEPENDENCIES",
            "META-INF/DEPENDENCIES.txt",
            "META-INF/beans.xml",
            "META-INF/io.netty.versions.properties",
            "META-INF/quarkus-config-roots.list",
            "META-INF/quarkus-javadoc.properties",
            "META-INF/quarkus-extension.properties",
            "META-INF/quarkus-extension.json",
            "META-INF/quarkus-extension.yaml",
            "META-INF/quarkus-deployment-dependency.graph",
            "META-INF/jandex.idx",
            "LICENSE")));

    private static final Logger log = Logger.getLogger(JarResultBuildStep.class);
    // we shouldn't have to specify these flags when opening a ZipFS (since they are the default ones), but failure to do so
    // makes a subsequent uberJar creation fail in java 8 (but works fine in Java 11)
    private static final OpenOption[] DEFAULT_OPEN_OPTIONS = { TRUNCATE_EXISTING, WRITE, CREATE };

    @BuildStep
    OutputTargetBuildItem outputTarget(BuildSystemTargetBuildItem bst, PackageConfig packageConfig) {
        String name = packageConfig.outputName.isPresent() ? packageConfig.outputName.get() : bst.getBaseName();
        Path path = packageConfig.outputDirectory.isPresent()
                ? bst.getOutputDirectory().resolve(packageConfig.outputDirectory.get())
                : bst.getOutputDirectory();
        return new OutputTargetBuildItem(path, name);
    }

    @BuildStep(onlyIf = JarRequired.class)
    ArtifactResultBuildItem jarOutput(JarBuildItem jarBuildItem) {
        if (jarBuildItem.getLibraryDir() != null) {
            return new ArtifactResultBuildItem(jarBuildItem.getPath(), PackageConfig.JAR,
                    Collections.singletonMap("library-dir", jarBuildItem.getLibraryDir()));
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
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            List<UberJarRequiredBuildItem> uberJarRequired,
            List<GeneratedFileSystemResourceBuildItem> generatedFileSystemResources) throws Exception {
        if (!uberJarRequired.isEmpty() || packageConfig.uberJar) {
            return buildUberJar(curateOutcomeBuildItem, outputTargetBuildItem, transformedClasses, applicationArchivesBuildItem,
                    packageConfig, applicationInfo, generatedClasses, generatedResources, generatedFileSystemResources);
        } else {
            return buildThinJar(curateOutcomeBuildItem, outputTargetBuildItem, transformedClasses, applicationArchivesBuildItem,
                    packageConfig, applicationInfo, generatedClasses, generatedResources, generatedFileSystemResources);
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
            List<GeneratedFileSystemResourceBuildItem> generatedFileSystemResources) throws Exception {

        //for uberjars we move the original jar, so there is only a single jar in the output directory
        Path standardJar = outputTargetBuildItem.getOutputDirectory().resolve(outputTargetBuildItem.getBaseName() + ".jar");
        Path originalJar = null;
        if (standardJar.toFile().exists()) {
            originalJar = outputTargetBuildItem.getOutputDirectory()
                    .resolve(outputTargetBuildItem.getBaseName() + ".jar.original");
            Files.deleteIfExists(originalJar);
            Files.move(standardJar, originalJar);
        }

        //we use the -runner jar name, unless we are building both types
        Path runnerJar = outputTargetBuildItem.getOutputDirectory()
                .resolve(outputTargetBuildItem.getBaseName() + packageConfig.runnerSuffix + ".jar");
        Files.deleteIfExists(runnerJar);

        try (FileSystem runnerZipFs = ZipUtils.newZip(runnerJar)) {

            log.info("Building fat jar: " + runnerJar);

            final Map<String, String> seen = new HashMap<>();
            final Map<String, Set<AppDependency>> duplicateCatcher = new HashMap<>();
            final StringBuilder classPath = new StringBuilder();
            final Map<String, List<byte[]>> services = new HashMap<>();
            Set<String> finalIgnoredEntries = new HashSet<>(IGNORED_ENTRIES);
            packageConfig.userConfiguredIgnoredEntries.ifPresent(finalIgnoredEntries::addAll);

            final List<AppDependency> appDeps = curateOutcomeBuildItem.getEffectiveModel().getUserDependencies();

            AppArtifact appArtifact = curateOutcomeBuildItem.getEffectiveModel().getAppArtifact();
            // the manifest needs to be the first entry in the jar, otherwise JarInputStream does not work properly
            // see https://bugs.openjdk.java.net/browse/JDK-8031748
            generateManifest(runnerZipFs, classPath.toString(), packageConfig, appArtifact, applicationInfo);

            for (AppDependency appDep : appDeps) {
                final AppArtifact depArtifact = appDep.getArtifact();
                final Path resolvedDep = depArtifact.getPath();

                // Exclude files that are not jars (typically, we can have XML files here, see https://github.com/quarkusio/quarkus/issues/2852)
                if (!resolvedDep.getFileName().toString().endsWith(".jar")) {
                    continue;
                }

                Set<String> transformedFromThisArchive = transformedClasses.getTransformedFilesByJar().get(resolvedDep);

                try (FileSystem artifactFs = ZipUtils.newFileSystem(resolvedDep)) {
                    for (final Path root : artifactFs.getRootDirectories()) {
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
                                        //if this has been transfomed we do not copy it
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
                                        boolean transformed = transformedFromThisArchive != null
                                                && transformedFromThisArchive.contains(relativePath);
                                        if (!transformed) {
                                            if (relativePath.startsWith("META-INF/services/") && relativePath.length() > 18) {
                                                services.computeIfAbsent(relativePath, (u) -> new ArrayList<>())
                                                        .add(read(file));
                                                return FileVisitResult.CONTINUE;
                                            } else if (!finalIgnoredEntries.contains(relativePath)) {
                                                duplicateCatcher.computeIfAbsent(relativePath, (a) -> new HashSet<>())
                                                        .add(appDep);
                                                if (!seen.containsKey(relativePath)) {
                                                    seen.put(relativePath, appDep.toString());
                                                    Files.copy(file, runnerZipFs.getPath(relativePath),
                                                            StandardCopyOption.REPLACE_EXISTING);
                                                } else if (!relativePath.endsWith(".class")) {
                                                    //for .class entries we warn as a group
                                                    log.warn("Duplicate entry " + relativePath + " entry from " + appDep
                                                            + " will be ignored. Existing file was provided by "
                                                            + seen.get(relativePath));
                                                }
                                            }
                                        }
                                        return FileVisitResult.CONTINUE;
                                    }
                                });
                    }

                }
            }
            Set<Set<AppDependency>> explained = new HashSet<>();
            for (Map.Entry<String, Set<AppDependency>> entry : duplicateCatcher.entrySet()) {
                if (entry.getValue().size() > 1) {
                    if (explained.add(entry.getValue())) {
                        log.warn("Dependencies with duplicate files detected. The dependencies " + entry.getValue()
                                + " contain duplicate files, e.g. " + entry.getKey());
                    }
                }
            }
            copyCommonContent(runnerZipFs, services, applicationArchivesBuildItem, transformedClasses, generatedClasses,
                    generatedResources, seen);
        }

        runnerJar.toFile().setReadable(true, false);

        generateFileSystemResources(outputTargetBuildItem, generatedFileSystemResources);

        return new JarBuildItem(runnerJar, originalJar, null);
    }

    private JarBuildItem buildThinJar(CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            PackageConfig packageConfig,
            ApplicationInfoBuildItem applicationInfo,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            List<GeneratedFileSystemResourceBuildItem> generatedFileSystemResources) throws Exception {

        Path runnerJar = outputTargetBuildItem.getOutputDirectory()
                .resolve(outputTargetBuildItem.getBaseName() + packageConfig.runnerSuffix + ".jar");
        Path libDir = outputTargetBuildItem.getOutputDirectory().resolve("lib");
        Files.deleteIfExists(runnerJar);
        IoUtils.recursiveDelete(libDir);
        Files.createDirectories(libDir);

        try (FileSystem runnerZipFs = ZipUtils.newZip(runnerJar)) {

            log.info("Building thin jar: " + runnerJar);

            doThinJarGeneration(curateOutcomeBuildItem, transformedClasses, applicationArchivesBuildItem, applicationInfo,
                    packageConfig, generatedResources, libDir, generatedClasses, runnerZipFs);
        }
        runnerJar.toFile().setReadable(true, false);

        generateFileSystemResources(outputTargetBuildItem, generatedFileSystemResources);

        return new JarBuildItem(runnerJar, null, libDir);
    }

    private void generateFileSystemResources(OutputTargetBuildItem outputTargetBuildItem,
            List<GeneratedFileSystemResourceBuildItem> generatedFileSystemResources) throws IOException {
        for (GeneratedFileSystemResourceBuildItem generatedFileSystemResource : generatedFileSystemResources) {
            Path outputPath = outputTargetBuildItem.getOutputDirectory().resolve(generatedFileSystemResource.getName());
            Files.createDirectories(outputPath.getParent());
            try (OutputStream out = Files.newOutputStream(outputPath)) {
                out.write(generatedFileSystemResource.getData());
            }
        }
    }

    /**
     * Native images are built from a specially created jar file. This allows for changes in how the jar file is generated.
     *
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
            List<GeneratedResourceBuildItem> generatedResources) throws Exception {
        Path thinJarDirectory = outputTargetBuildItem.getOutputDirectory()
                .resolve(outputTargetBuildItem.getBaseName() + "-native-image-source-jar");
        IoUtils.recursiveDelete(thinJarDirectory);
        Files.createDirectories(thinJarDirectory);
        copyJsonConfigFiles(applicationArchivesBuildItem, thinJarDirectory);

        Path runnerJar = thinJarDirectory
                .resolve(outputTargetBuildItem.getBaseName() + packageConfig.runnerSuffix + ".jar");
        Path libDir = thinJarDirectory.resolve("lib");
        Files.createDirectories(libDir);

        List<GeneratedClassBuildItem> allClasses = new ArrayList<>(generatedClasses);
        allClasses.addAll(nativeImageResources.stream()
                .map((s) -> new GeneratedClassBuildItem(true, s.getName(), s.getClassData())).collect(Collectors.toList()));

        try (FileSystem runnerZipFs = ZipUtils.newZip(runnerJar)) {

            log.info("Building native image source jar: " + runnerJar);

            doThinJarGeneration(curateOutcomeBuildItem, transformedClasses, applicationArchivesBuildItem, applicationInfo,
                    packageConfig, generatedResources, libDir, allClasses, runnerZipFs);
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
        // this will contain all the resources in both maven and gradle cases - the latter is true because we copy them in AugmentTask
        Path classesLocation = applicationArchivesBuildItem.getRootArchive().getArchiveLocation();
        try (Stream<Path> stream = Files.find(classesLocation, 1, new BiPredicate<Path, BasicFileAttributes>() {
            @Override
            public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
                return basicFileAttributes.isRegularFile() && path.toString().endsWith(".json");
            }
        })) {
            stream.forEach(new Consumer<Path>() {
                @Override
                public void accept(Path jsonPath) {
                    try {
                        Files.copy(jsonPath, thinJarDirectory.resolve(jsonPath.getFileName()));
                    } catch (IOException e) {
                        throw new UncheckedIOException(
                                "Unable to copy json config file from " + jsonPath + " to " + thinJarDirectory,
                                e);
                    }
                }
            });
        }
    }

    private void doThinJarGeneration(CurateOutcomeBuildItem curateOutcomeBuildItem,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            List<GeneratedResourceBuildItem> generatedResources,
            Path libDir,
            List<GeneratedClassBuildItem> allClasses,
            FileSystem runnerZipFs)
            throws BootstrapDependencyProcessingException, AppModelResolverException, IOException {
        final Map<String, String> seen = new HashMap<>();
        final StringBuilder classPath = new StringBuilder();
        final Map<String, List<byte[]>> services = new HashMap<>();

        final List<AppDependency> appDeps = curateOutcomeBuildItem.getEffectiveModel().getUserDependencies();

        copyLibraryJars(transformedClasses, libDir, classPath, appDeps);

        AppArtifact appArtifact = curateOutcomeBuildItem.getEffectiveModel().getAppArtifact();
        // the manifest needs to be the first entry in the jar, otherwise JarInputStream does not work properly
        // see https://bugs.openjdk.java.net/browse/JDK-8031748
        generateManifest(runnerZipFs, classPath.toString(), packageConfig, appArtifact, applicationInfo);
        copyCommonContent(runnerZipFs, services, applicationArchivesBuildItem, transformedClasses, allClasses,
                generatedResources, seen);
    }

    private void copyLibraryJars(TransformedClassesBuildItem transformedClasses, Path libDir,
            StringBuilder classPath, List<AppDependency> appDeps) throws IOException {
        for (AppDependency appDep : appDeps) {
            final AppArtifact depArtifact = appDep.getArtifact();
            final Path resolvedDep = depArtifact.getPath();

            // Exclude files that are not jars (typically, we can have XML files here, see https://github.com/quarkusio/quarkus/issues/2852)
            if (!resolvedDep.getFileName().toString().endsWith(".jar")) {
                continue;
            }

            Set<String> transformedFromThisArchive = transformedClasses.getTransformedFilesByJar().get(resolvedDep);

            if (transformedFromThisArchive == null || transformedFromThisArchive.isEmpty()) {
                final String fileName = depArtifact.getGroupId() + "." + resolvedDep.getFileName();
                final Path targetPath = libDir.resolve(fileName);
                Files.copy(resolvedDep, targetPath, StandardCopyOption.REPLACE_EXISTING);
                classPath.append(" lib/" + fileName);
            } else {
                //we have transformed classes, we need to handle them correctly
                final String fileName = "modified-" + depArtifact.getGroupId() + "." + resolvedDep.getFileName();
                final Path targetPath = libDir.resolve(fileName);
                classPath.append(" lib/" + fileName);
                filterZipFile(resolvedDep, targetPath, transformedFromThisArchive);
            }

        }
    }

    private void copyCommonContent(FileSystem runnerZipFs, Map<String, List<byte[]>> services,
            ApplicationArchivesBuildItem appArchives, TransformedClassesBuildItem transformedClassesBuildItem,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources, Map<String, String> seen)
            throws IOException {

        //TODO: this is probably broken in gradle
        //        if (Files.exists(augmentOutcome.getConfigDir())) {
        //            copyFiles(augmentOutcome.getConfigDir(), runnerZipFs, services);
        //        }
        for (Set<TransformedClassesBuildItem.TransformedClass> transformed : transformedClassesBuildItem
                .getTransformedClassesByJar().values()) {
            for (TransformedClassesBuildItem.TransformedClass i : transformed) {
                Path target = runnerZipFs.getPath(i.getFileName());
                handleParent(runnerZipFs, i.getFileName(), seen);
                try (final OutputStream out = wrapForJDK8232879(Files.newOutputStream(target, DEFAULT_OPEN_OPTIONS))) {
                    out.write(i.getData());
                }
                seen.put(i.getFileName(), "Current Application");
            }
        }
        for (GeneratedClassBuildItem i : generatedClasses) {
            String fileName = i.getName().replace(".", "/") + ".class";
            seen.put(fileName, "Current Application");
            Path target = runnerZipFs.getPath(fileName);
            handleParent(runnerZipFs, fileName, seen);
            if (Files.exists(target)) {
                continue;
            }
            try (final OutputStream os = wrapForJDK8232879(Files.newOutputStream(target, DEFAULT_OPEN_OPTIONS))) {
                os.write(i.getClassData());
            }
        }

        for (GeneratedResourceBuildItem i : generatedResources) {
            Path target = runnerZipFs.getPath(i.getName());
            handleParent(runnerZipFs, i.getName(), seen);
            if (Files.exists(target)) {
                continue;
            }
            if (i.getName().startsWith("META-INF/services")) {
                services.computeIfAbsent(i.getName(), (u) -> new ArrayList<>()).add(i.getClassData());
            } else {
                try (final OutputStream os = wrapForJDK8232879(Files.newOutputStream(target, DEFAULT_OPEN_OPTIONS))) {
                    os.write(i.getClassData());
                }
            }
        }

        copyFiles(appArchives.getRootArchive().getArchiveRoot(), runnerZipFs, services);

        for (Map.Entry<String, List<byte[]>> entry : services.entrySet()) {
            try (final OutputStream os = wrapForJDK8232879(
                    Files.newOutputStream(runnerZipFs.getPath(entry.getKey()), DEFAULT_OPEN_OPTIONS))) {
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
     * Otherwise this manifest manipulation will be useless.
     */
    private void generateManifest(FileSystem runnerZipFs, final String classPath, PackageConfig config, AppArtifact appArtifact,
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
        if (attributes.containsKey(Attributes.Name.CLASS_PATH)) {
            log.warn(
                    "Your MANIFEST.MF already defined a CLASS_PATH entry. Quarkus has overwritten this existing entry.");
        }
        attributes.put(Attributes.Name.CLASS_PATH, classPath);
        if (attributes.containsKey(Attributes.Name.MAIN_CLASS)) {
            String existingMainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
            if (!config.mainClass.equals(existingMainClass)) {
                log.warn("Your MANIFEST.MF already defined a MAIN_CLASS entry. Quarkus has overwritten your existing entry.");
            }
        }
        attributes.put(Attributes.Name.MAIN_CLASS, config.mainClass);
        if (config.manifest.addImplementationEntries && !attributes.containsKey(Attributes.Name.IMPLEMENTATION_TITLE)) {
            String name = ApplicationInfoBuildItem.UNSET_VALUE.equals(applicationInfo.getName()) ? appArtifact.getArtifactId()
                    : applicationInfo.getName();
            attributes.put(Attributes.Name.IMPLEMENTATION_TITLE, name);
        }
        if (config.manifest.addImplementationEntries && !attributes.containsKey(Attributes.Name.IMPLEMENTATION_VERSION)) {
            String version = ApplicationInfoBuildItem.UNSET_VALUE.equals(applicationInfo.getVersion())
                    ? appArtifact.getVersion()
                    : applicationInfo.getVersion();
            attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, version);
        }
        try (final OutputStream os = wrapForJDK8232879(Files.newOutputStream(manifestPath, DEFAULT_OPEN_OPTIONS))) {
            manifest.write(os);
        }
    }

    /**
     * Copy files from {@code dir} to {@code fs}, filtering out service providers into the given map.
     *
     * @param dir the source directory
     * @param fs the destination filesystem
     * @param services the services map
     * @throws IOException if an error occurs
     */
    private void copyFiles(Path dir, FileSystem fs, Map<String, List<byte[]>> services) throws IOException {
        try (Stream<Path> fileTreeElements = Files.walk(dir)) {
            fileTreeElements.forEach(new Consumer<Path>() {
                @Override
                public void accept(Path path) {
                    final Path file = dir.relativize(path);
                    final String relativePath = toUri(file);
                    if (relativePath.isEmpty()) {
                        return;
                    }
                    try {
                        if (Files.isDirectory(path)) {
                            addDir(fs, relativePath);
                        } else {
                            if (relativePath.startsWith("META-INF/services/") && relativePath.length() > 18) {
                                final byte[] content;
                                try {
                                    content = Files.readAllBytes(path);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                services.computeIfAbsent(relativePath, (u) -> new ArrayList<>()).add(content);
                            } else {
                                Path target = fs.getPath(relativePath);
                                if (!Files.exists(target)) {
                                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
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
            throws IOException, FileAlreadyExistsException {
        final Path targetDir = fs.getPath(relativePath);
        try {
            Files.createDirectory(targetDir);
        } catch (FileAlreadyExistsException e) {
            if (!Files.isDirectory(targetDir)) {
                throw e;
            }
        }
    }

    private static byte[] read(Path p) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int r;
        try (InputStream in = Files.newInputStream(p)) {
            while ((r = in.read(buffer)) > 0) {
                out.write(buffer, 0, r);
            }
        }
        return out.toByteArray();
    }

    private static String toUri(Path path) {
        if (path.isAbsolute()) {
            return path.toUri().getPath();
        } else if (path.getNameCount() == 0) {
            return "";
        } else {
            return toUri(new StringBuilder(), path, 0).toString();
        }
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
            return packageConfig.type.equalsIgnoreCase(PackageConfig.JAR);
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
}
