package io.quarkus.bootstrap.app;

import java.util.function.Function;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

/**
 * Strategy for managing augmentation class loaders across {@link CuratedApplication} instances.
 * <p>
 * Implementations control whether augmentation class loaders are shared (to save Metaspace)
 * or standalone (one per {@code CuratedApplication}).
 *
 * @see DefaultAugmentationClassLoaderManager
 * @see SharedAugmentationClassLoaderManager
 */
public interface AugmentationClassLoaderManager {

    /**
     * Get or create the augmentation class loader.
     * <p>
     * The supplied factory builds the class loader from the given input.
     * Implementations may call it immediately or return a previously built shared instance.
     *
     * @return the class loader and its element cache, never {@code null}
     */
    AugmentationClassLoaderResult getOrCreateAugmentationClassLoader(AugmentationClassLoaderInput input,
            Function<AugmentationClassLoaderInput, AugmentationClassLoaderResult> factory);

    /**
     * Release the augmentation class loader.
     * <p>
     * For standalone managers, this closes the class loader immediately.
     * For shared managers, this decrements the reference count and closes only when the last
     * reference is released. If the class loader is not the shared instance (e.g. due to
     * incompatible configs), it is closed directly.
     *
     * @param augmentClassLoader the class loader to release
     */
    void release(QuarkusClassLoader augmentClassLoader);
}
