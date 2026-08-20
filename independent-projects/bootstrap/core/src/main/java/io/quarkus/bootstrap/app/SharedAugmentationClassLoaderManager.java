package io.quarkus.bootstrap.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

/**
 * Manages shared augmentation class loaders across multiple {@link CuratedApplication} instances.
 * <p>
 * When running tests with many profiles, each profile creates its own {@code CuratedApplication}
 * and augmentation class loader. Without sharing, each instance loads the same deployment classes
 * into its own class loader, consuming significant Metaspace memory.
 * <p>
 * This manager holds shared augmentation class loaders that are built by the first
 * {@code CuratedApplication} requesting a given configuration. Subsequent instances with
 * compatible configurations reuse the same class loader, so deployment classes are loaded only once
 * per distinct configuration.
 * <p>
 * Each shared class loader uses reference counting: it is closed only when the last
 * {@code CuratedApplication} releases its reference.
 * <p>
 * Instances of this class should be created by the test framework and passed to
 * {@link QuarkusBootstrap.Builder#setAugmentationClassLoaderManager(AugmentationClassLoaderManager)}.
 * The instance should be scoped to a single module's test execution (e.g., one Surefire/Failsafe run),
 * we don't want to share the class loader between runs.
 */
public class SharedAugmentationClassLoaderManager implements AugmentationClassLoaderManager {

    private static final Logger log = Logger.getLogger(SharedAugmentationClassLoaderManager.class);

    private final Object lock = new Object();

    private final List<SharedAugmentationClassLoader> sharedClassLoaders = new ArrayList<>();

    @Override
    public AugmentationClassLoaderResult getOrCreateAugmentationClassLoader(AugmentationClassLoaderInput input,
            Function<AugmentationClassLoaderInput, AugmentationClassLoaderResult> factory) {
        synchronized (lock) {
            for (SharedAugmentationClassLoader shared : sharedClassLoaders) {
                if (shared.input.isCompatibleWith(input)) {
                    shared.refCount++;
                    log.debugf("Reusing shared augmentation class loader (refCount=%d)", shared.refCount);
                    return shared.result;
                }
            }

            AugmentationClassLoaderResult result = factory.apply(input);
            sharedClassLoaders.add(new SharedAugmentationClassLoader(input, result));
            log.debugf("Created shared augmentation class loader (total=%d)", sharedClassLoaders.size());
            return result;
        }
    }

    @Override
    public void release(QuarkusClassLoader augmentClassLoader) {
        synchronized (lock) {
            for (int i = 0; i < sharedClassLoaders.size(); i++) {
                SharedAugmentationClassLoader shared = sharedClassLoaders.get(i);
                if (augmentClassLoader == shared.result.classLoader()) {
                    shared.refCount--;
                    if (shared.refCount == 0) {
                        log.debug("Closing shared augmentation class loader (last reference released)");
                        shared.result.classLoader().close();
                        sharedClassLoaders.remove(i);
                    } else {
                        log.debugf("Released shared augmentation class loader reference (refCount=%d)", shared.refCount);
                    }
                    return;
                }
            }

            augmentClassLoader.close();
        }
    }

    private static class SharedAugmentationClassLoader {

        final AugmentationClassLoaderInput input;
        final AugmentationClassLoaderResult result;
        int refCount;

        SharedAugmentationClassLoader(AugmentationClassLoaderInput input, AugmentationClassLoaderResult result) {
            this.input = input;
            this.result = result;
            this.refCount = 1;
        }
    }
}
