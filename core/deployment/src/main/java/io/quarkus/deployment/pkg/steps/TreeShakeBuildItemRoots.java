package io.quarkus.deployment.pkg.steps;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ForceNonWeakReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeRootBytecodeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeRootClassBuildItem;

/**
 * Maps build items to tree-shake roots.
 */
final class TreeShakeBuildItemRoots {

    private TreeShakeBuildItemRoots() {
    }

    /**
     * Roots from build items describing dynamic access performed at runtime in any build mode:
     * reflection, JNI and service providers. Weak reflective class registrations are not roots,
     * since they only apply if the class is reachable anyway, unless a {@link ForceNonWeakReflectiveClassBuildItem}
     * makes them unconditional.
     *
     * @param roots receives the root classes, possibly more than once
     */
    static void allModes(
            List<ReflectiveClassBuildItem> reflectiveClasses,
            List<ForceNonWeakReflectiveClassBuildItem> forcedNonWeakClasses,
            List<ReflectiveFieldBuildItem> reflectiveFields,
            List<ReflectiveMethodBuildItem> reflectiveMethods,
            List<JniRuntimeAccessBuildItem> jniClasses,
            List<JniRuntimeAccessFieldBuildItem> jniFields,
            List<JniRuntimeAccessMethodBuildItem> jniMethods,
            List<ServiceProviderBuildItem> serviceProviderItems,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        final Set<String> forcedNonWeakClassNames = forcedNonWeakClassNames(forcedNonWeakClasses);
        for (ReflectiveClassBuildItem item : reflectiveClasses) {
            if (!item.isWeak()) {
                for (String className : item.getClassNames()) {
                    roots.produce(new JarTreeShakeRootClassBuildItem(className));
                }
            } else if (!forcedNonWeakClassNames.isEmpty()) {
                for (String className : item.getClassNames()) {
                    if (forcedNonWeakClassNames.contains(className)) {
                        roots.produce(new JarTreeShakeRootClassBuildItem(className));
                    }
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
            for (String className : item.providers()) {
                roots.produce(new JarTreeShakeRootClassBuildItem(className));
            }
        }
    }

    /**
     * Names of the classes whose weak reflective registrations are forced to be unconditional.
     */
    private static Set<String> forcedNonWeakClassNames(List<ForceNonWeakReflectiveClassBuildItem> items) {
        if (items.isEmpty()) {
            return Set.of();
        }
        final Set<String> names = new HashSet<>(items.size());
        for (ForceNonWeakReflectiveClassBuildItem item : items) {
            names.add(item.getClassName());
        }
        return names;
    }

    /**
     * Roots from build items that are only meaningful to {@code native-image}:
     * class initialization settings, GraalVM Features and security providers.
     *
     * @param roots receives the root classes, possibly more than once
     */
    static void nativeOnly(
            List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses,
            List<RuntimeReinitializedClassBuildItem> runtimeReinitializedClasses,
            List<NativeImageFeatureBuildItem> nativeImageFeatures,
            List<NativeImageConfigBuildItem> nativeImageConfigs,
            List<NativeImageSecurityProviderBuildItem> securityProviders,
            BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        for (RuntimeInitializedClassBuildItem item : runtimeInitializedClasses) {
            roots.produce(new JarTreeShakeRootClassBuildItem(item.getClassName()));
        }
        for (RuntimeReinitializedClassBuildItem item : runtimeReinitializedClasses) {
            roots.produce(new JarTreeShakeRootClassBuildItem(item.getClassName()));
        }
        for (NativeImageFeatureBuildItem item : nativeImageFeatures) {
            roots.produce(new JarTreeShakeRootClassBuildItem(item.getQualifiedName()));
        }
        for (NativeImageConfigBuildItem item : nativeImageConfigs) {
            for (String className : item.getRuntimeInitializedClasses()) {
                roots.produce(new JarTreeShakeRootClassBuildItem(className));
            }
        }
        for (NativeImageSecurityProviderBuildItem item : securityProviders) {
            roots.produce(new JarTreeShakeRootClassBuildItem(item.getSecurityProvider()));
        }
    }

    /**
     * Root bytecode from classes generated only for {@code native-image}, such as GraalVM Features.
     * They aren't on the build classpath, so the classes they reference are kept by tracing their bytecode.
     *
     * @param rootBytecode receives the bytecode of the generated classes
     */
    static void generatedNativeImageClasses(List<GeneratedNativeImageClassBuildItem> generatedClasses,
            BuildProducer<JarTreeShakeRootBytecodeBuildItem> rootBytecode) {
        for (GeneratedNativeImageClassBuildItem item : generatedClasses) {
            // the name may be given as a binary name, with '/' separators
            rootBytecode.produce(
                    new JarTreeShakeRootBytecodeBuildItem(item.getName().replace('/', '.'), item.getClassData()));
        }
    }
}
