package io.quarkus.deployment.pkg.steps;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassConditionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.TreeShakeConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeExcludedArtifactBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeRootClassBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.runtime.LaunchMode;

public class JarTreeShakeProcessor {

    private static final Logger log = Logger.getLogger(JarTreeShakeProcessor.class);

    static class TreeShakeEnabled implements BooleanSupplier {
        private final PackageConfig packageConfig;
        private final LaunchMode launchMode;

        TreeShakeEnabled(PackageConfig packageConfig, LaunchMode launchMode) {
            this.packageConfig = packageConfig;
            this.launchMode = launchMode;
        }

        @Override
        public boolean getAsBoolean() {
            return launchMode == LaunchMode.NORMAL
                    && packageConfig.jar().treeShake().mode() == TreeShakeConfig.TreeShakeMode.CLASSES
                    && packageConfig.jar().type() != PackageConfig.JarConfig.JarType.MUTABLE_JAR;
        }
    }

    @BuildStep
    void collectConfiguredExcludedArtifacts(
            PackageConfig packageConfig,
            BuildProducer<JarTreeShakeExcludedArtifactBuildItem> excludedArtifacts) {
        packageConfig.jar().treeShake().excludedArtifacts().ifPresent(artifacts -> {
            for (String coords : artifacts) {
                excludedArtifacts.produce(new JarTreeShakeExcludedArtifactBuildItem(
                        ArtifactKey.fromString(coords)));
            }
        });
    }

    @BuildStep(onlyIfNot = TreeShakeEnabled.class)
    void skipTreeShaking(BuildProducer<JarTreeShakeBuildItem> treeShakeProducer) {
        treeShakeProducer
                .produce(new JarTreeShakeBuildItem(false, Set.of(),
                        Map.of(), Set.of()));
    }

    @BuildStep(onlyIf = TreeShakeEnabled.class)
    void analyzeReachableClasses(
            PackageConfig packageConfig,
            CurateOutcomeBuildItem curateOutcome,
            List<GeneratedClassBuildItem> generatedClasses,
            TransformedClassesBuildItem transformedClasses,
            List<ReflectiveClassConditionBuildItem> reflectiveClassConditions,
            List<JarTreeShakeRootClassBuildItem> rootClasses,
            List<JarTreeShakeExcludedArtifactBuildItem> excludedArtifacts,
            BuildProducer<JarTreeShakeBuildItem> treeShakeProducer) {

        try (JarTreeShakeInput input = JarTreeShakeInput.collect(
                packageConfig.jar().treeShake().mode(),
                curateOutcome.getApplicationModel(),
                generatedClasses,
                transformedClasses,
                reflectiveClassConditions,
                rootClasses,
                excludedArtifacts)) {
            treeShakeProducer.produce(new JarTreeShaker(input).run());
        }
    }

    @BuildStep
    void collectGeneratedClassRoots(
            MainClassBuildItem mainClass,
            List<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        roots.produce(new JarTreeShakeRootClassBuildItem(mainClass.getClassName()));
        for (GeneratedClassBuildItem gen : generatedClasses) {
            roots.produce(new JarTreeShakeRootClassBuildItem(gen.binaryName().replace('/', '.')));
        }
    }

    /**
     * Scans generated resources for {@code .classlist} files and adds the class names
     * they contain as tree-shake roots. These files are produced by extension deployment
     * processors (e.g., H2's {@code H2JDBCReflections}) and list classes that are loaded
     * by name at native image build time via GraalVM {@code Feature} implementations.
     * Currently only produced for native builds (guarded by {@code NativeOrNativeSourcesBuild}).
     */
    @BuildStep
    void collectClassListResourceRoots(
            List<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        for (GeneratedResourceBuildItem resource : generatedResources) {
            if (resource.getName().endsWith(".classlist")) {
                String content = new String(resource.getData(), java.nio.charset.StandardCharsets.UTF_8);
                for (String line : content.split("\n")) {
                    String className = line.trim();
                    if (!className.isEmpty()) {
                        roots.produce(new JarTreeShakeRootClassBuildItem(className));
                    }
                }
            }
        }
    }

    @BuildStep
    void collectReflectionRoots(
            List<ReflectiveClassBuildItem> reflectiveClasses,
            List<ReflectiveFieldBuildItem> reflectiveFields,
            List<ReflectiveMethodBuildItem> reflectiveMethods,
            List<JniRuntimeAccessBuildItem> jniClasses,
            List<JniRuntimeAccessFieldBuildItem> jniFields,
            List<JniRuntimeAccessMethodBuildItem> jniMethods,
            List<ServiceProviderBuildItem> serviceProviderItems,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        TreeShakeBuildItemRoots.allModes(reflectiveClasses, reflectiveFields, reflectiveMethods,
                jniClasses, jniFields, jniMethods, serviceProviderItems, rootProducer(roots));
    }

    /**
     * Class initialization settings and GraalVM Features are only meaningful to {@code native-image},
     * so they are roots only when building a native executable.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void collectNativeOnlyBuildItemRoots(
            List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses,
            List<RuntimeReinitializedClassBuildItem> runtimeReinitializedClasses,
            List<NativeImageFeatureBuildItem> nativeImageFeatures,
            List<NativeImageConfigBuildItem> nativeImageConfigs,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        TreeShakeBuildItemRoots.nativeOnly(runtimeInitializedClasses, runtimeReinitializedClasses,
                nativeImageFeatures, nativeImageConfigs, rootProducer(roots));
    }

    /**
     * Scans dependency JARs for {@code META-INF/native-image/} configuration files
     * and extracts class names that must be preserved.
     * See {@link NativeImageConfigRootCollector} for which files apply to which build mode.
     */
    @BuildStep
    void collectNativeImageConfigRoots(
            NativeConfig nativeConfig,
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        final boolean nativeTarget = nativeConfig.enabled();
        final Consumer<String> rootProducer = rootProducer(roots);
        for (ResolvedDependency dep : curateOutcome.getApplicationModel().getDependencies(DependencyFlags.RUNTIME_CP)) {
            NativeImageConfigRootCollector.collect(dep.getContentTree(), nativeTarget, rootProducer);
        }
    }

    private static Consumer<String> rootProducer(BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        return className -> roots.produce(new JarTreeShakeRootClassBuildItem(className));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void collectNativeImageRoots(
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        /*
         * Scans dependency JARs for classes that reference GraalVM/SVM annotations
         * ({@code @TargetClass}, {@code @Substitute}, {@code @InjectAccessors}, etc.).
         * These substitution classes are discovered by GraalVM through classpath scanning,
         * not through normal code references, so they must be explicitly preserved as roots.
         */
        byte[] svmMarker = "com/oracle/svm/core/annotate/".getBytes(StandardCharsets.UTF_8);
        for (ResolvedDependency dep : curateOutcome.getApplicationModel().getDependencies(DependencyFlags.RUNTIME_CP)) {
            try (OpenPathTree openTree = dep.getContentTree().open()) {
                openTree.walk(visit -> {
                    String path = visit.getResourceName();
                    if (path.endsWith(".class") && !path.equals("module-info.class")) {
                        try {
                            byte[] classBytes = Files.readAllBytes(visit.getPath());
                            if (containsBytes(classBytes, svmMarker)) {
                                String className = path.substring(0, path.length() - 6).replace('/', '.');
                                roots.produce(new JarTreeShakeRootClassBuildItem(className));
                            }
                        } catch (IOException e) {
                            log.debugf(e, "Failed to read class file: %s", path);
                        }
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read content of " + dep.toCompactCoords(), e);
            }
        }
    }

    private static boolean containsBytes(byte[] haystack, byte[] needle) {
        outer: for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
