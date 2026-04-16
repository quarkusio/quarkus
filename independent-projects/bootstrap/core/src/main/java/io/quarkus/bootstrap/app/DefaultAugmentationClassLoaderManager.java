package io.quarkus.bootstrap.app;

import java.util.function.Function;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

/**
 * Default (non-sharing) augmentation class loader manager.
 * <p>
 * Each {@link CuratedApplication} using this manager gets its own augmentation class loader.
 * The class loader is built by calling the factory and closed on {@link #release(QuarkusClassLoader)}.
 * <p>
 * This implementation is stateless and can safely be shared across multiple
 * {@code CuratedApplication} instances without risk of interleaving.
 */
public class DefaultAugmentationClassLoaderManager implements AugmentationClassLoaderManager {

    private static final DefaultAugmentationClassLoaderManager INSTANCE = new DefaultAugmentationClassLoaderManager();

    public static DefaultAugmentationClassLoaderManager getInstance() {
        return INSTANCE;
    }

    @Override
    public AugmentationClassLoaderResult getOrCreateAugmentationClassLoader(AugmentationClassLoaderInput input,
            Function<AugmentationClassLoaderInput, AugmentationClassLoaderResult> factory) {
        return factory.apply(input);
    }

    @Override
    public void release(QuarkusClassLoader augmentClassLoader) {
        augmentClassLoader.close();
    }
}
