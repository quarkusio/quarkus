package io.quarkus.arc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.arc.impl.ArcContainerImpl;

/**
 * Provides access to the ArC container.
 */
public final class Arc {

    private static final AtomicReference<ArcContainerImpl> INSTANCE = new AtomicReference<>();

    /**
     * Initializes {@link ArcContainer} with default settings.
     * This is equal to using {@code Arc#initialize(ArcInitConfig.INSTANCE)}
     *
     * @return {@link ArcContainer} instance with default configuration
     */
    public static ArcContainer initialize() {
        return initialize(ArcInitConfig.INSTANCE);
    }

    /**
     * Deprecated, will be removed in future iterations.
     * Users are encouraged to use {@link #initialize(ArcInitConfig)} instead.
     *
     * @param currentContextFactory
     * @return the initialized container
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public static ArcContainer initialize(CurrentContextFactory currentContextFactory) {
        return initialize(ArcInitConfig.builder().setCurrentContextFactory(currentContextFactory).build());
    }

    public static ArcContainer initialize(ArcInitConfig arcInitConfig) {
        ArcContainerImpl container = INSTANCE.get();
        if (container == null) {
            synchronized (INSTANCE) {
                container = INSTANCE.get();
                if (container == null) {
                    // Set the container instance first because Arc.container() can be used within ArcContainerImpl.init()
                    container = new ArcContainerImpl(arcInitConfig.getCurrentContextFactory(),
                            arcInitConfig.isStrictCompatibility());
                    INSTANCE.set(container);
                    container.init();
                }
            }
        }
        return container;
    }

    public static void setExecutor(ExecutorService executor) {
        INSTANCE.get().setExecutor(executor);
    }

    /**
     *
     * @return the container instance
     */
    public static ArcContainer container() {
        return INSTANCE.get();
    }

    public static void shutdown() {
        ArcContainerImpl container = INSTANCE.get();
        if (container != null) {
            synchronized (INSTANCE) {
                container = INSTANCE.get();
                if (container != null) {
                    container.shutdown();
                    INSTANCE.set(null);
                }
            }
        }
    }

}
