package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

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

public class JarTreeShakeProcessor {

    private static final Logger log = Logger.getLogger(JarTreeShakeProcessor.class);

    private static final String META_INF_NATIVE_IMAGE = "META-INF/native-image/";

    static class TreeShakeEnabled implements BooleanSupplier {
        private final PackageConfig packageConfig;

        TreeShakeEnabled(PackageConfig packageConfig) {
            this.packageConfig = packageConfig;
        }

        @Override
        public boolean getAsBoolean() {
            return packageConfig.jar().treeShake().mode() == TreeShakeConfig.TreeShakeMode.CLASSES
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
                        Map.of()));
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
            List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses,
            List<RuntimeReinitializedClassBuildItem> runtimeReinitializedClasses,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        for (ReflectiveClassBuildItem item : reflectiveClasses) {
            if (!item.isWeak()) {
                for (String className : item.getClassNames()) {
                    roots.produce(new JarTreeShakeRootClassBuildItem(className));
                }
            }
        }
        for (ReflectiveFieldBuildItem item : reflectiveFields) {
            roots.produce(new JarTreeShakeRootClassBuildItem(item.getDeclaringClass()));
        }
        for (ReflectiveMethodBuildItem item : reflectiveMethods) {
            roots.produce(new JarTreeShakeRootClassBuildItem(item.getDeclaringClass()));
        }
        for (JniRuntimeAccessBuildItem item : jniClasses) {
            for (String className : item.getClassNames()) {
                roots.produce(new JarTreeShakeRootClassBuildItem(className));
            }
        }
        for (JniRuntimeAccessFieldBuildItem item : jniFields) {
            roots.produce(new JarTreeShakeRootClassBuildItem(item.getDeclaringClass()));
        }
        for (JniRuntimeAccessMethodBuildItem item : jniMethods) {
            roots.produce(new JarTreeShakeRootClassBuildItem(item.getDeclaringClass()));
        }
        for (ServiceProviderBuildItem item : serviceProviderItems) {
            for (String provider : item.providers()) {
                roots.produce(new JarTreeShakeRootClassBuildItem(provider));
            }
        }
        for (RuntimeInitializedClassBuildItem item : runtimeInitializedClasses) {
            roots.produce(new JarTreeShakeRootClassBuildItem(item.getClassName()));
        }
        for (RuntimeReinitializedClassBuildItem item : runtimeReinitializedClasses) {
            roots.produce(new JarTreeShakeRootClassBuildItem(item.getClassName()));
        }
    }

    /**
     * Scans dependency JARs for {@code META-INF/native-image/} configuration files
     * and extracts class names that must be preserved for native image builds.
     * This includes classes referenced in {@code native-image.properties}
     * ({@code --initialize-at-run-time}, {@code --initialize-at-build-time})
     * and in {@code reflect-config.json} / {@code jni-config.json}.
     */
    @BuildStep
    void collectNativeImageConfigRoots(
            CurateOutcomeBuildItem curateOutcome,
            List<NativeImageFeatureBuildItem> nativeImageFeatures,
            List<NativeImageConfigBuildItem> nativeImageConfigs,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {

        for (NativeImageFeatureBuildItem item : nativeImageFeatures) {
            roots.produce(new JarTreeShakeRootClassBuildItem(item.getQualifiedName()));
        }
        for (NativeImageConfigBuildItem item : nativeImageConfigs) {
            for (String className : item.getRuntimeInitializedClasses()) {
                roots.produce(new JarTreeShakeRootClassBuildItem(className));
            }
        }

        for (ResolvedDependency dep : curateOutcome.getApplicationModel().getDependencies(DependencyFlags.RUNTIME_CP)) {
            try (OpenPathTree openTree = dep.getContentTree().open()) {
                openTree.walkIfContains(META_INF_NATIVE_IMAGE, visit -> {
                    String path = visit.getResourceName();
                    if (path.endsWith("native-image.properties")) {
                        parseNativeImageProperties(visit.getPath(), roots);
                    } else if (path.endsWith("reflect-config.json") || path.endsWith("jni-config.json")) {
                        parseJsonClassConfig(visit.getPath(), roots);
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read content of " + dep.toCompactCoords(), e);
            }
        }
    }

    /**
     * Parses a {@code native-image.properties} file and extracts class names from
     * {@code --initialize-at-run-time}, {@code --initialize-at-build-time}, and
     * {@code --features} arguments.
     */
    private static void parseNativeImageProperties(java.nio.file.Path file,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder args = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                if (!args.isEmpty()) {
                    args.append(' ');
                }
                // handle line continuations
                if (line.endsWith("\\")) {
                    args.append(line, 0, line.length() - 1);
                } else {
                    args.append(line);
                }
            }
            String argsStr = args.toString();
            extractClassesFromArg(argsStr, "--initialize-at-run-time=", roots);
            extractClassesFromArg(argsStr, "--initialize-at-build-time=", roots);
            extractClassesFromArg(argsStr, "--features=", roots);
        } catch (IOException e) {
            log.debugf(e, "Failed to read native-image.properties: %s", file);
        }
    }

    private static void extractClassesFromArg(String args, String prefix,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        int idx = 0;
        while ((idx = args.indexOf(prefix, idx)) >= 0) {
            int start = idx + prefix.length();
            int end = args.indexOf(' ', start);
            String value = end < 0 ? args.substring(start) : args.substring(start, end);
            for (String className : value.split(",")) {
                className = className.trim();
                if (!className.isEmpty()) {
                    roots.produce(new JarTreeShakeRootClassBuildItem(className));
                }
            }
            idx = start;
        }
    }

    /**
     * Parses a {@code reflect-config.json} or {@code jni-config.json} file and
     * extracts class names referenced in it.
     */
    private static void parseJsonClassConfig(java.nio.file.Path file,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        try (InputStream is = Files.newInputStream(file);
                JsonReader jsonReader = Json.createReader(is)) {
            JsonArray array = jsonReader.readArray();
            for (JsonValue entry : array) {
                if (entry.getValueType() == JsonValue.ValueType.OBJECT) {
                    JsonObject obj = entry.asJsonObject();
                    String name = obj.getString("name", null);
                    if (name != null && !name.isEmpty()) {
                        roots.produce(new JarTreeShakeRootClassBuildItem(name));
                    }
                }
            }
        } catch (IOException e) {
            log.debugf(e, "Failed to read native-image config: %s", file);
        }
    }

    @BuildStep
    void collectNativeImageRoots(
            NativeConfig nativeConfig,
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        if (!nativeConfig.enabled()) {
            return;
        }

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
