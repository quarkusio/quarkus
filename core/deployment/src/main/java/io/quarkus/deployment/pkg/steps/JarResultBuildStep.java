package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType.MUTABLE_JAR;
import static io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType.UBER_JAR;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

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
import io.quarkus.deployment.jvm.ResolvedJVMRequirements;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveRequestedBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarIgnoredResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarMergedResourceBuildItem;
import io.quarkus.deployment.pkg.jar.FastJarBuilder;
import io.quarkus.deployment.pkg.jar.LegacyThinJarBuilder;
import io.quarkus.deployment.pkg.jar.NativeImageSourceJarBuilder;
import io.quarkus.deployment.pkg.jar.UberJarBuilder;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACT;

/**
 * This build step builds both the thin jars and uber jars.
 * <p>
 * The way this is built is a bit convoluted. In general, we only want a single one built,
 * as determined by the {@link PackageConfig} (unless the config explicitly asks for both of them)
 * <p>
 * However, we still need an extension to be able to ask for a specific one of these despite the config,
 * e.g. if a serverless environment needs an uberjar to build its deployment package then we need
 * to be able to provide this.
 * <p>
 * To enable this we have two build steps that strongly produce the respective artifact type build
 * items, but not a {@link ArtifactResultBuildItem}. We then
 * have another two build steps that only run if they are configured to consume these explicit
 * build items and transform them into {@link ArtifactResultBuildItem}.
 */
public class JarResultBuildStep {

    @BuildStep
    OutputTargetBuildItem outputTarget(BuildSystemTargetBuildItem bst, PackageConfig packageConfig) {
        String name = packageConfig.outputName().orElseGet(bst::getBaseName);
        Path path = packageConfig.outputDirectory().map(s -> bst.getOutputDirectory().resolve(s))
                .orElseGet(bst::getOutputDirectory);
        Optional<Set<ArtifactKey>> includedOptionalDependencies;
        if (packageConfig.jar().filterOptionalDependencies()) {
            includedOptionalDependencies = Optional.of(packageConfig.jar().includedOptionalDependencies()
                    .map(set -> set.stream().map(ArtifactKey.class::cast).collect(Collectors.toSet()))
                    .orElse(Collections.emptySet()));
        } else {
            includedOptionalDependencies = Optional.empty();
        }
        return new OutputTargetBuildItem(path, name, bst.getOriginalBaseName(), bst.isRebuild(), bst.getBuildSystemProps(),
                includedOptionalDependencies);
    }

    @BuildStep(onlyIf = JarRequired.class)
    ArtifactResultBuildItem jarOutput(JarBuildItem jarBuildItem) {
        return new ArtifactResultBuildItem(jarBuildItem.getPath(), "jar",
                jarBuildItem.getLibraryDir() == null ? Map.of()
                        : Map.of("library-dir", jarBuildItem.getLibraryDir().toString()),
                jarBuildItem.getManifestConfig());
    }

    @SuppressWarnings("deprecation") // JarType#LEGACY_JAR
    @BuildStep
    public JarBuildItem buildRunnerJar(CurateOutcomeBuildItem curateOutcomeBuildItem,
            ResolvedJVMRequirements jvmRequirements,
            OutputTargetBuildItem outputTargetBuildItem,
            TransformedClassesBuildItem transformedClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            ClassLoadingConfig classLoadingConfig,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            List<UberJarMergedResourceBuildItem> uberJarMergedResourceBuildItems,
            List<UberJarIgnoredResourceBuildItem> uberJarIgnoredResourceBuildItems,
            QuarkusBuildCloseablesBuildItem closeablesBuildItem,
            List<AdditionalApplicationArchiveBuildItem> additionalApplicationArchiveBuildItems,
            MainClassBuildItem mainClassBuildItem,
            Optional<JvmStartupOptimizerArchiveRequestedBuildItem> jvmStartupOptimizerArchiveRequested,
            ExecutorService buildExecutor)
            throws Exception {

        if (jvmStartupOptimizerArchiveRequested.isPresent()) {
            handleAppCDSSupportFileGeneration(transformedClasses, generatedClasses, jvmStartupOptimizerArchiveRequested.get());
        }

        Set<ArtifactKey> removedArtifactKeys = getRemovedArtifactKeys(classLoadingConfig);
        Set<ArtifactKey> parentFirstArtifactKeys = getParentFirstArtifactKeys(curateOutcomeBuildItem, classLoadingConfig);

        return switch (packageConfig.jar().type()) {
            case UBER_JAR -> new UberJarBuilder(curateOutcomeBuildItem,
                    outputTargetBuildItem,
                    applicationInfo,
                    packageConfig,
                    mainClassBuildItem,
                    applicationArchivesBuildItem,
                    transformedClasses,
                    generatedClasses,
                    generatedResources,
                    removedArtifactKeys,
                    uberJarMergedResourceBuildItems,
                    uberJarIgnoredResourceBuildItems,
                    jvmRequirements).build();
            case LEGACY_JAR -> new LegacyThinJarBuilder(curateOutcomeBuildItem,
                    outputTargetBuildItem,
                    applicationInfo,
                    packageConfig,
                    mainClassBuildItem,
                    applicationArchivesBuildItem,
                    transformedClasses,
                    generatedClasses,
                    generatedResources,
                    removedArtifactKeys,
                    buildExecutor,
                    jvmRequirements).build();
            case FAST_JAR, MUTABLE_JAR -> new FastJarBuilder(curateOutcomeBuildItem,
                    outputTargetBuildItem,
                    applicationInfo,
                    packageConfig,
                    mainClassBuildItem,
                    applicationArchivesBuildItem,
                    additionalApplicationArchiveBuildItems,
                    transformedClasses,
                    generatedClasses,
                    generatedResources,
                    parentFirstArtifactKeys,
                    removedArtifactKeys,
                    buildExecutor,
                    jvmRequirements).build();
        };
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
            ClassLoadingConfig classLoadingConfig,
            ExecutorService buildExecutor,
            ResolvedJVMRequirements jvmRequirements) throws Exception {

        return new NativeImageSourceJarBuilder(curateOutcomeBuildItem,
                outputTargetBuildItem,
                applicationInfo,
                packageConfig,
                mainClassBuildItem,
                applicationArchivesBuildItem,
                transformedClasses,
                generatedClasses,
                generatedResources,
                nativeImageResources,
                getRemovedArtifactKeys(classLoadingConfig),
                buildExecutor,
                jvmRequirements).build();
    }

    // the idea here is to just dump the class names of the generated and transformed classes into a file
    // that is read at runtime when AppCDS generation is requested
    private void handleAppCDSSupportFileGeneration(TransformedClassesBuildItem transformedClasses,
            List<GeneratedClassBuildItem> generatedClasses,
            JvmStartupOptimizerArchiveRequestedBuildItem jvmStartupOptimizerArchiveRequested)
            throws IOException {
        Path dir = jvmStartupOptimizerArchiveRequested.getDir();
        Path generatedClassesFile = dir.resolve("generatedAndTransformed.lst");
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

            if (!classes.isEmpty()) {
                writer.write(classes.toString());
            }
        }
    }

    static class JarRequired implements BooleanSupplier {

        private final PackageConfig packageConfig;

        JarRequired(PackageConfig packageConfig) {
            this.packageConfig = packageConfig;
        }

        @Override
        public boolean getAsBoolean() {
            return packageConfig.jar().enabled();
        }
    }

    /**
     * @return a {@code Set} containing the key of the artifacts to load from the parent ClassLoader first.
     */
    private static Set<ArtifactKey> getParentFirstArtifactKeys(CurateOutcomeBuildItem curateOutcomeBuildItem,
            ClassLoadingConfig classLoadingConfig) {
        final Set<ArtifactKey> parentFirstKeys = new HashSet<>();
        curateOutcomeBuildItem.getApplicationModel().getDependencies().forEach(d -> {
            if (d.isFlagSet(DependencyFlags.CLASSLOADER_RUNNER_PARENT_FIRST)) {
                parentFirstKeys.add(d.getKey());
            }
        });
        classLoadingConfig.parentFirstArtifacts().ifPresent(
                parentFirstArtifacts -> {
                    for (String artifact : parentFirstArtifacts) {
                        parentFirstKeys.add(new GACT(artifact.split(":")));
                    }
                });
        return parentFirstKeys;
    }

    private static Set<ArtifactKey> getRemovedArtifactKeys(ClassLoadingConfig classLoadingConfig) {
        if (classLoadingConfig.removedArtifacts().isEmpty()) {
            return Set.of();
        }

        Set<ArtifactKey> removedArtifacts = new HashSet<>();
        for (String artifact : classLoadingConfig.removedArtifacts().get()) {
            removedArtifacts.add(GACT.fromString(artifact));
        }
        return Collections.unmodifiableSet(removedArtifacts);
    }
}
