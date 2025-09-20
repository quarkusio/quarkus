package io.quarkus.deployment.pkg.jar;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;
import static io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType.MUTABLE_JAR;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.MutableJarApplicationModel;
import io.quarkus.bootstrap.runner.QuarkusEntryPoint;
import io.quarkus.bootstrap.runner.SerializedApplication;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.deployment.ResolvedJVMRequirements;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem.TransformedClass;
import io.quarkus.deployment.pkg.JarUnsigner;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.sbom.ApplicationComponent;
import io.quarkus.sbom.ApplicationManifestConfig;

public class FastJarBuilder extends AbstractJarBuilder<JarBuildItem> {

    private static final Logger LOG = Logger.getLogger(FastJarBuilder.class);

    private static final String MP_CONFIG_FILE = "META-INF/microprofile-config.properties";

    private final List<AdditionalApplicationArchiveBuildItem> additionalApplicationArchives;
    private final Set<ArtifactKey> parentFirstArtifactKeys;
    private final ExecutorService executorService;

    public FastJarBuilder(CurateOutcomeBuildItem curateOutcome,
            OutputTargetBuildItem outputTarget,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            MainClassBuildItem mainClass,
            ApplicationArchivesBuildItem applicationArchives,
            List<AdditionalApplicationArchiveBuildItem> additionalApplicationArchives,
            TransformedClassesBuildItem transformedClasses,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            Set<ArtifactKey> parentFirstArtifactKeys,
            Set<ArtifactKey> removedArtifactKeys,
            ExecutorService executorService,
            ResolvedJVMRequirements jvmRequirements) {
        super(curateOutcome, outputTarget, applicationInfo, packageConfig, mainClass, applicationArchives, transformedClasses,
                generatedClasses, generatedResources, removedArtifactKeys, jvmRequirements);
        this.additionalApplicationArchives = additionalApplicationArchives;
        this.parentFirstArtifactKeys = parentFirstArtifactKeys;
        this.executorService = executorService;
    }

    public JarBuildItem build() throws IOException {
        boolean rebuild = outputTarget.isRebuild();

        Path buildDir;

        if (packageConfig.outputDirectory().isPresent()) {
            buildDir = outputTarget.getOutputDirectory();
        } else {
            buildDir = outputTarget.getOutputDirectory().resolve(FastJarFormat.DEFAULT_FAST_JAR_DIRECTORY_NAME);
        }

        final ApplicationManifestConfig.Builder manifestConfig = ApplicationManifestConfig.builder()
                .setApplicationModel(curateOutcome.getApplicationModel())
                .setDistributionDirectory(buildDir);
        //unmodified 3rd party dependencies
        Path libDir = buildDir.resolve(FastJarFormat.LIB);
        Path mainLib = libDir.resolve(FastJarFormat.MAIN);
        //parent first entries
        Path baseLib = libDir.resolve(FastJarFormat.BOOT_LIB);
        Files.createDirectories(baseLib);

        Path appDir = buildDir.resolve(FastJarFormat.APP);
        Path quarkus = buildDir.resolve(FastJarFormat.QUARKUS);
        Path userProviders = null;
        if (packageConfig.jar().userProvidersDirectory().isPresent()) {
            userProviders = buildDir.resolve(packageConfig.jar().userProvidersDirectory().get());
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
        PackageConfig.DecompilerConfig decompilerConfig = packageConfig.jar().decompiler();
        if (decompilerConfig.enabled()) {
            decompiledOutputDir = buildDir.getParent().resolve(decompilerConfig.outputDirectory());
            FileUtil.deleteDirectory(decompiledOutputDir);
            Files.createDirectory(decompiledOutputDir);
            decompiler = new VineflowerDecompiler();
            Path jarDirectory = Paths.get(decompilerConfig.jarDirectory());
            if (!Files.exists(jarDirectory)) {
                Files.createDirectory(jarDirectory);
            }
            decompiler.init(new Decompiler.Context(jarDirectory, decompiledOutputDir));
            decompiler.downloadIfNecessary();
        }

        FastJarJars.FastJarJarsBuilder fastJarJarsBuilder = new FastJarJars.FastJarJarsBuilder();
        List<Path> parentFirst = new ArrayList<>();
        //we process in order of priority
        //transformed classes first
        if (!transformedClasses.getTransformedClassesByJar().isEmpty()) {
            Path transformedZip = quarkus.resolve(FastJarFormat.TRANSFORMED_BYTECODE_JAR);
            fastJarJarsBuilder.setTransformedJar(transformedZip);
            try (ArchiveCreator archiveCreator = new ParallelCommonsCompressArchiveCreator(transformedZip,
                    packageConfig.jar().compress(), packageConfig.outputTimestamp().orElse(null),
                    outputTarget.getOutputDirectory(), executorService)) {
                // we make sure the entries are added in a reproducible order
                // we use Path#toString() to get a reproducible order on both Unix-based OSes and Windows
                for (Entry<Path, Set<TransformedClass>> transformedClassEntry : transformedClasses
                        .getTransformedClassesByJar().entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getKey().toString())).toList()) {
                    for (TransformedClass transformed : transformedClassEntry.getValue().stream()
                            .sorted(Comparator.comparing(TransformedClass::getFileName)).toList()) {
                        if (transformed.getData() != null) {
                            archiveCreator.addFile(transformed.getData(), transformed.getFileName());
                        }
                    }
                }
            }
            if (decompiler != null) {
                wasDecompiledSuccessfully = decompiler.decompile(transformedZip);
            }
        }
        //now generated classes and resources
        Path generatedZip = quarkus.resolve(FastJarFormat.GENERATED_BYTECODE_JAR);
        fastJarJarsBuilder.setGeneratedJar(generatedZip);
        try (ArchiveCreator archiveCreator = new ParallelCommonsCompressArchiveCreator(generatedZip,
                packageConfig.jar().compress(), packageConfig.outputTimestamp().orElse(null), outputTarget.getOutputDirectory(),
                executorService)) {
            // make sure we write the elements in order
            for (GeneratedClassBuildItem i : generatedClasses.stream()
                    .sorted(Comparator.comparing(GeneratedClassBuildItem::binaryName)).toList()) {
                String fileName = fromClassNameToResourceName(i.internalName());
                archiveCreator.addFile(i.getClassData(), fileName);
            }

            // make sure we write the elements in order
            for (GeneratedResourceBuildItem i : generatedResources.stream()
                    .sorted(Comparator.comparing(GeneratedResourceBuildItem::getName)).toList()) {
                archiveCreator.addFile(i.getData(), i.getName());
            }
        }
        if (decompiler != null) {
            wasDecompiledSuccessfully &= decompiler.decompile(generatedZip);
        }

        if (wasDecompiledSuccessfully && (decompiledOutputDir != null)) {
            LOG.info("The decompiled output can be found at: " + decompiledOutputDir.toAbsolutePath().toString());
        }

        //now the application classes
        Path runnerJar = appDir.resolve(outputTarget.getBaseName() + DOT_JAR);
        fastJarJarsBuilder.setRunnerJar(runnerJar);

        if (!rebuild) {
            manifestConfig.addComponent(ApplicationComponent.builder()
                    .setResolvedDependency(applicationArchives.getRootArchive().getResolvedDependency())
                    .setPath(runnerJar));
            Predicate<String> ignoredEntriesPredicate = getThinJarIgnoredEntriesPredicate(packageConfig);
            try (ArchiveCreator archiveCreator = new ParallelCommonsCompressArchiveCreator(runnerJar,
                    packageConfig.jar().compress(), packageConfig.outputTimestamp().orElse(null),
                    outputTarget.getOutputDirectory(), executorService)) {
                copyFiles(applicationArchives.getRootArchive(), archiveCreator, null, ignoredEntriesPredicate);
            }
        }
        final StringBuilder classPath = new StringBuilder();
        final Map<ArtifactKey, List<Path>> copiedArtifacts = new HashMap<>();
        for (ResolvedDependency appDep : curateOutcome.getApplicationModel().getRuntimeDependencies()) {
            if (!rebuild) {
                copyDependency(parentFirstArtifactKeys, outputTarget, copiedArtifacts, mainLib, baseLib,
                        fastJarJarsBuilder::addDependency, true,
                        classPath, appDep, transformedClasses, removedArtifactKeys, packageConfig, manifestConfig,
                        executorService);
            } else if (includeAppDependency(appDep, outputTarget.getIncludedOptionalDependencies(), removedArtifactKeys)) {
                appDep.getResolvedPaths().forEach(fastJarJarsBuilder::addDependency);
            }
            if (parentFirstArtifactKeys.contains(appDep.getKey())) {
                appDep.getResolvedPaths().forEach(parentFirst::add);
            }
        }
        for (AdditionalApplicationArchiveBuildItem i : additionalApplicationArchives) {
            for (Path path : i.getResolvedPaths()) {
                if (!path.getParent().equals(userProviders)) {
                    throw new RuntimeException(
                            "Additional application archives can only be provided from the user providers directory. " + path
                                    + " is not present in " + userProviders);
                }
                fastJarJarsBuilder.addDependency(path);
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
            if (fastJarJars.transformedJar != null) {
                allJars.add(fastJarJars.transformedJar);
            }
            allJars.add(fastJarJars.generatedJar);
            allJars.add(fastJarJars.runnerJar);
            List<Path> sortedDeps = new ArrayList<>(fastJarJars.dependencies);
            Collections.sort(sortedDeps);
            allJars.addAll(sortedDeps);
            List<Path> sortedParentFirst = new ArrayList<>(parentFirst);
            Collections.sort(sortedParentFirst);
            List<String> sortedNonExistentResources = new ArrayList<>(nonExistentResources);
            Collections.sort(sortedNonExistentResources);
            SerializedApplication.write(out, mainClass.getClassName(), buildDir, allJars, sortedParentFirst,
                    sortedNonExistentResources);
        }

        runnerJar.toFile().setReadable(true, false);
        Path initJar = buildDir.resolve(FastJarFormat.QUARKUS_RUN_JAR);
        manifestConfig.setMainComponent(ApplicationComponent.builder()
                .setPath(initJar)
                .setDependencies(List.of(curateOutcome.getApplicationModel().getAppArtifact())))
                .setRunnerPath(initJar);
        boolean mutableJar = packageConfig.jar().type() == MUTABLE_JAR;
        if (mutableJar) {
            //we output the properties in a reproducible manner, so we remove the date comment
            //and sort them
            //we still use Properties to get the escaping right though, so basically we write out the lines
            //to memory, split them, discard comments, sort them, then write them to disk
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            outputTarget.getBuildSystemProperties().store(out, null);
            List<String> lines = Arrays.stream(out.toString(StandardCharsets.UTF_8).split("\n"))
                    .filter(s -> !s.startsWith("#")).sorted().collect(Collectors.toList());
            Path buildSystemProps = quarkus.resolve(FastJarFormat.BUILD_SYSTEM_PROPERTIES);
            manifestConfig.addComponent(ApplicationComponent.builder().setPath(buildSystemProps).setDevelopmentScope());
            try (OutputStream fileOutput = Files.newOutputStream(buildSystemProps)) {
                fileOutput.write(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
            }
        }
        if (!rebuild) {
            try (ArchiveCreator archiveCreator = new ParallelCommonsCompressArchiveCreator(initJar,
                    packageConfig.jar().compress(), packageConfig.outputTimestamp().orElse(null),
                    outputTarget.getOutputDirectory(), executorService)) {
                ResolvedDependency appArtifact = curateOutcome.getApplicationModel().getAppArtifact();
                generateManifest(archiveCreator, classPath.toString(), packageConfig, appArtifact,
                        jvmRequirements,
                        QuarkusEntryPoint.class.getName(),
                        applicationInfo);
            }

            //now copy the deployment artifacts, if required
            if (mutableJar) {
                Path deploymentLib = libDir.resolve(FastJarFormat.DEPLOYMENT_LIB);
                Files.createDirectories(deploymentLib);
                for (ResolvedDependency appDep : curateOutcome.getApplicationModel().getDependencies()) {
                    copyDependency(parentFirstArtifactKeys, outputTarget, copiedArtifacts, deploymentLib, baseLib, p -> {
                    }, false, classPath, appDep, new TransformedClassesBuildItem(Map.of()), removedArtifactKeys, packageConfig,
                            manifestConfig, executorService); //we don't care about transformation here, so just pass in an empty item
                }
                Map<ArtifactKey, List<String>> relativePaths = new HashMap<>();
                for (Map.Entry<ArtifactKey, List<Path>> e : copiedArtifacts.entrySet()) {
                    relativePaths.put(e.getKey(),
                            e.getValue().stream().map(s -> buildDir.relativize(s).toString().replace('\\', '/'))
                                    .collect(Collectors.toList()));
                }

                //now we serialize the data needed to build up the reaugmentation class path
                //first the app model
                MutableJarApplicationModel model = new MutableJarApplicationModel(outputTarget.getBaseName(),
                        relativePaths,
                        curateOutcome.getApplicationModel(),
                        packageConfig.jar().userProvidersDirectory().orElse(null), buildDir.relativize(runnerJar).toString());
                Path appmodelDat = deploymentLib.resolve(FastJarFormat.APPMODEL_DAT);
                manifestConfig.addComponent(ApplicationComponent.builder().setPath(appmodelDat).setDevelopmentScope());
                try (OutputStream out = Files.newOutputStream(appmodelDat)) {
                    ObjectOutputStream obj = new ObjectOutputStream(out);
                    obj.writeObject(model);
                    obj.close();
                }
                //now the bootstrap CP
                //we just include all deployment deps, even though we only really need bootstrap
                //as we don't really have a resolved bootstrap CP
                //once we have the app model it will all be done in QuarkusClassLoader anyway
                Path deploymentCp = deploymentLib.resolve(FastJarFormat.DEPLOYMENT_CLASS_PATH_DAT);
                manifestConfig.addComponent(ApplicationComponent.builder().setPath(deploymentCp).setDevelopmentScope());
                try (OutputStream out = Files.newOutputStream(deploymentCp)) {
                    ObjectOutputStream obj = new ObjectOutputStream(out);
                    List<String> paths = new ArrayList<>();
                    for (ResolvedDependency i : curateOutcome.getApplicationModel().getDependencies()) {
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

            if (packageConfig.jar().includeDependencyList()) {
                Path deplist = buildDir.resolve(FastJarFormat.QUARKUS_APP_DEPS);
                List<String> lines = new ArrayList<>();
                for (ResolvedDependency i : curateOutcome.getApplicationModel().getRuntimeDependencies()) {
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
        return new JarBuildItem(initJar, null, libDir, packageConfig.jar().type(), null, manifestConfig.build());
    }

    private static Predicate<String> getThinJarIgnoredEntriesPredicate(PackageConfig packageConfig) {
        return packageConfig.jar().userConfiguredIgnoredEntries().map(Set::copyOf).orElse(Set.of())::contains;
    }

    private static void copyDependency(Set<ArtifactKey> parentFirstArtifacts, OutputTargetBuildItem outputTargetBuildItem,
            Map<ArtifactKey, List<Path>> runtimeArtifacts, Path libDir, Path baseLib, Consumer<Path> targetPathConsumer,
            boolean allowParentFirst, StringBuilder classPath, ResolvedDependency appDep,
            TransformedClassesBuildItem transformedClasses, Set<ArtifactKey> removedDeps,
            PackageConfig packageConfig, ApplicationManifestConfig.Builder manifestConfig, ExecutorService executorService)
            throws IOException {

        // Exclude files that are not jars (typically, we can have XML files here, see https://github.com/quarkusio/quarkus/issues/2852)
        // and are not part of the optional dependencies to include
        if (!includeAppDependency(appDep, outputTargetBuildItem.getIncludedOptionalDependencies(), removedDeps)) {
            return;
        }
        if (runtimeArtifacts.containsKey(appDep.getKey())) {
            return;
        }
        for (Path resolvedDep : appDep.getResolvedPaths()) {
            final String fileName = FastJarFormat.getJarFileName(appDep, resolvedDep);
            final Path targetPath;

            if (allowParentFirst && parentFirstArtifacts.contains(appDep.getKey())) {
                targetPath = baseLib.resolve(fileName);
                classPath.append(" ").append(FastJarFormat.LIB).append("/").append(FastJarFormat.BOOT_LIB).append("/")
                        .append(fileName);
            } else {
                targetPath = libDir.resolve(fileName);
                targetPathConsumer.accept(targetPath);
            }
            runtimeArtifacts.computeIfAbsent(appDep.getKey(), (s) -> new ArrayList<>(1)).add(targetPath);

            if (Files.isDirectory(resolvedDep)) {
                // This case can happen when we are building a jar from inside the Quarkus repository
                // and Quarkus Bootstrap's localProjectDiscovery has been set to true. In such a case
                // the non-jar dependencies are the Quarkus dependencies picked up on the file system
                packageClasses(resolvedDep, targetPath, packageConfig, outputTargetBuildItem, executorService);
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
                var appComponent = ApplicationComponent.builder()
                        .setPath(targetPath)
                        .setResolvedDependency(appDep);
                if (removedFromThisArchive.isEmpty()) {
                    Files.copy(resolvedDep, targetPath, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                } else {
                    // we copy jars for which we remove entries to the same directory
                    // which seems a bit odd to me
                    JarUnsigner.unsignJar(resolvedDep, targetPath, Predicate.not(removedFromThisArchive::contains));

                    var list = new ArrayList<>(removedFromThisArchive);
                    Collections.sort(list);
                    var sb = new StringBuilder("Removed ").append(list.get(0));
                    for (int i = 1; i < list.size(); ++i) {
                        sb.append(",").append(list.get(i));
                    }
                    appComponent.setPedigree(sb.toString());
                }
                manifestConfig.addComponent(appComponent);
            }
        }
    }

    private static void packageClasses(Path resolvedDep, final Path targetPath, PackageConfig packageConfig,
            OutputTargetBuildItem outputTargetBuildItem, ExecutorService executorService) throws IOException {
        try (ArchiveCreator archiveCreator = new ParallelCommonsCompressArchiveCreator(targetPath,
                packageConfig.jar().compress(), packageConfig.outputTimestamp().orElse(null),
                outputTargetBuildItem.getOutputDirectory(), executorService)) {
            Files.walkFileTree(resolvedDep, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                throws IOException {
                            final Path relativePath = resolvedDep.relativize(file);
                            archiveCreator.addFile(file, relativePath.toString()); //replace only needed for testing
                            return FileVisitResult.CONTINUE;
                        }
                    });
        }
    }

    private static class FastJarJars {
        private final Path transformedJar;
        private final Path generatedJar;
        private final Path runnerJar;
        private final List<Path> dependencies;

        public FastJarJars(FastJarJarsBuilder builder) {
            this.transformedJar = builder.transformedJar;
            this.generatedJar = builder.generatedJar;
            this.runnerJar = builder.runnerJar;
            this.dependencies = builder.dependencies;
        }

        public static class FastJarJarsBuilder {
            private Path transformedJar;
            private Path generatedJar;
            private Path runnerJar;
            private final List<Path> dependencies = new ArrayList<>();

            public FastJarJarsBuilder setTransformedJar(Path transformedJar) {
                this.transformedJar = transformedJar;
                return this;
            }

            public FastJarJarsBuilder setGeneratedJar(Path generatedJar) {
                this.generatedJar = generatedJar;
                return this;
            }

            public FastJarJarsBuilder setRunnerJar(Path runnerJar) {
                this.runnerJar = runnerJar;
                return this;
            }

            public FastJarJarsBuilder addDependency(Path dependency) {
                this.dependencies.add(dependency);
                return this;
            }

            public FastJarJars build() {
                return new FastJarJars(this);
            }
        }
    }
}
