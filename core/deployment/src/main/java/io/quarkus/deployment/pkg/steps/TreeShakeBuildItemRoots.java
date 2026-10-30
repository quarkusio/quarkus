package io.quarkus.deployment.pkg.steps;

import java.util.List;
import java.util.function.Consumer;

import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

/**
 * Maps build items to tree-shake roots.
 */
final class TreeShakeBuildItemRoots {

    private TreeShakeBuildItemRoots() {
    }

    /**
     * Roots from build items describing dynamic access performed at runtime in any build mode:
     * reflection, JNI and service providers. Weak reflective class registrations are not roots,
     * since they only apply if the class is reachable anyway.
     *
     * @param roots receives the dot-separated names of root classes, possibly more than once
     */
    static void allModes(
            List<ReflectiveClassBuildItem> reflectiveClasses,
            List<ReflectiveFieldBuildItem> reflectiveFields,
            List<ReflectiveMethodBuildItem> reflectiveMethods,
            List<JniRuntimeAccessBuildItem> jniClasses,
            List<JniRuntimeAccessFieldBuildItem> jniFields,
            List<JniRuntimeAccessMethodBuildItem> jniMethods,
            List<ServiceProviderBuildItem> serviceProviderItems,
            Consumer<String> roots) {
        for (ReflectiveClassBuildItem item : reflectiveClasses) {
            if (!item.isWeak()) {
                for (String className : item.getClassNames()) {
                    roots.accept(className);
                }
            }
        }
        for (ReflectiveFieldBuildItem item : reflectiveFields) {
            roots.accept(item.getDeclaringClass());
        }
        for (ReflectiveMethodBuildItem item : reflectiveMethods) {
            roots.accept(item.getDeclaringClass());
        }
        for (JniRuntimeAccessBuildItem item : jniClasses) {
            for (String className : item.getClassNames()) {
                roots.accept(className);
            }
        }
        for (JniRuntimeAccessFieldBuildItem item : jniFields) {
            roots.accept(item.getDeclaringClass());
        }
        for (JniRuntimeAccessMethodBuildItem item : jniMethods) {
            roots.accept(item.getDeclaringClass());
        }
        for (ServiceProviderBuildItem item : serviceProviderItems) {
            for (String className : item.providers()) {
                roots.accept(className);
            }
        }
    }

    /**
     * Roots from build items that are only meaningful to {@code native-image}:
     * class initialization settings and GraalVM Features.
     *
     * @param roots receives the dot-separated names of root classes, possibly more than once
     */
    static void nativeOnly(
            List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses,
            List<RuntimeReinitializedClassBuildItem> runtimeReinitializedClasses,
            List<NativeImageFeatureBuildItem> nativeImageFeatures,
            List<NativeImageConfigBuildItem> nativeImageConfigs,
            Consumer<String> roots) {
        for (RuntimeInitializedClassBuildItem item : runtimeInitializedClasses) {
            roots.accept(item.getClassName());
        }
        for (RuntimeReinitializedClassBuildItem item : runtimeReinitializedClasses) {
            roots.accept(item.getClassName());
        }
        for (NativeImageFeatureBuildItem item : nativeImageFeatures) {
            roots.accept(item.getQualifiedName());
        }
        for (NativeImageConfigBuildItem item : nativeImageConfigs) {
            for (String className : item.getRuntimeInitializedClasses()) {
                roots.accept(className);
            }
        }
    }
}
