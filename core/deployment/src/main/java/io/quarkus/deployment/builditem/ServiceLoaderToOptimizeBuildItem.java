package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Requests build-time optimization of {@link java.util.ServiceLoader} usage in a specific class.
 * <p>
 * The processor will analyze the class bytecode, auto-detect each {@code ServiceLoader.load()} call site,
 * extract the service interface from the {@code LDC} instruction, resolve providers at build time via
 * {@link io.quarkus.deployment.util.ServiceUtil}, and rewrite the call to avoid runtime classpath scanning.
 * <p>
 * Each call site is handled independently. If auto-detection or resolution fails for a specific call site,
 * that call is left unchanged while others may still be optimized.
 */
public final class ServiceLoaderToOptimizeBuildItem extends MultiBuildItem {

    private final String classToTransform;

    public ServiceLoaderToOptimizeBuildItem(String classToTransform) {
        this.classToTransform = classToTransform;
    }

    public String getClassToTransform() {
        return classToTransform;
    }
}
