package io.quarkus.bootstrap.app;

import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

/**
 * Manages a shared augmentation class loader across multiple {@link CuratedApplication} instances.
 * <p>
 * When running tests with many profiles, each profile creates its own {@code CuratedApplication}
 * and augmentation class loader. Without sharing, each instance loads the same deployment classes
 * into its own class loader, consuming significant Metaspace memory.
 * <p>
 * This manager holds a single shared augmentation class loader that is built by the first
 * {@code CuratedApplication} using the standard augmentation class loader construction logic.
 * Subsequent instances reuse the same class loader, provided they are compatible,
 * so deployment classes are loaded only once.
 * <p>
 * The shared class loader uses reference counting: it is closed only when the last
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

    private AugmentationClassLoaderResult sharedResult;
    private AugmentationClassLoaderInput sharedInput;
    private int refCount;

    /**
     * Get or create the augmentation class loader.
     * <p>
     * If no shared class loader exists yet, the supplied factory is called to build one.
     * The factory is only called once; subsequent calls return the already-built result.
     * <p>
     * If the caller's {@link AugmentationClassLoaderInput} is incompatible with the input
     * used to build the shared class loader, a standalone class loader is built using the
     * factory and returned. This standalone class loader will be closed directly on
     * {@link #release(QuarkusClassLoader)}.
     * <p>
     * Each successful call must be paired with a call to
     * {@link #release(QuarkusClassLoader)}.
     *
     * @param input all inputs that determine the augmentation class loader contents
     * @param factory builds the augmentation class loader and element cache from the input
     * @return the class loader and its element cache, never {@code null}
     */
    @Override
    public AugmentationClassLoaderResult getOrCreateAugmentationClassLoader(AugmentationClassLoaderInput input,
            Function<AugmentationClassLoaderInput, AugmentationClassLoaderResult> factory) {
        synchronized (lock) {
            if (sharedResult == null) {
                sharedResult = factory.apply(input);
                sharedInput = input;
                refCount++;
                log.debug("Created shared augmentation class loader (refCount=1)");
                return sharedResult;
            }
            if (!sharedInput.isCompatibleWith(input)) {
                log.debug("Incompatible input, building standalone augmentation class loader");
                return factory.apply(input);
            }
            refCount++;
            log.debugf("Reusing shared augmentation class loader (refCount=%d)", refCount);
            return sharedResult;
        }
    }

    /**
     * Release the augmentation class loader.
     * <p>
     * If the class loader is the shared instance, the reference count is decremented.
     * When the last reference is released, the shared class loader is closed.
     * <p>
     * If the class loader is not the shared instance (e.g. due to incompatible configs),
     * it is closed directly.
     */
    @Override
    public void release(QuarkusClassLoader augmentClassLoader) {
        synchronized (lock) {
            if (sharedResult == null || augmentClassLoader != sharedResult.classLoader()) {
                augmentClassLoader.close();
                return;
            }
            refCount--;
            if (refCount == 0) {
                log.debug("Closing shared augmentation class loader (last reference released)");
                sharedResult.classLoader().close();
                sharedResult = null;
                sharedInput = null;
            } else {
                log.debugf("Released shared augmentation class loader reference (refCount=%d)", refCount);
            }
        }
    }
}
