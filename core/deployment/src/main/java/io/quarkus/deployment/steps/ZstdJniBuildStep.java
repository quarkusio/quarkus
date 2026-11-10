package io.quarkus.deployment.steps;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

/**
 * zstd-jni is an optional dependency of several libraries (Kafka clients, Netty, ...).
 * <p>
 * Starting with 1.5.7-18, zstd-jni is a multi-release jar which, on Java 22+, uses the FFM API instead of JNI.
 * Its downcall method handles are stored in static final fields which would end up as constants in the image heap
 * if initialized at build time, making the native image build fail with
 * {@code unexpected input could not be handled: linkToNative}.
 * <p>
 * zstd-jni is not supported in native mode, so we runtime initialize the whole library to keep the build working.
 */
public class ZstdJniBuildStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void runtimeInitializeZstdJni(BuildProducer<RuntimeInitializedPackageBuildItem> runtimeInitializedPackages) {
        if (QuarkusClassLoader.isClassPresentAtRuntime("com.github.luben.zstd.Zstd")) {
            runtimeInitializedPackages.produce(new RuntimeInitializedPackageBuildItem("com.github.luben.zstd"));
        }
    }
}
