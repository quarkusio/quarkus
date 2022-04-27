package io.quarkus.arc;

import io.quarkus.arc.impl.ArcContainerImpl;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides access to the ArC container.
 */
public final class Arc {

    private static final AtomicReference<ArcContainerImpl> INSTANCE = new AtomicReference<>();

    public static ArcContainer initialize() {
        return initialize(null);
    }

    /**
     *
     * @param currentContextFactory
     * @return the initialized container
     */
    public static ArcContainer initialize(CurrentContextFactory currentContextFactory) {
        ArcContainerImpl container = INSTANCE.get();
        if (container == null) {
            synchronized (INSTANCE) {
                container = INSTANCE.get();
                if (container == null) {
                    // Set the container instance first because Arc.container() can be used within ArcContainerImpl.init()
                    container = new ArcContainerImpl(currentContextFactory);
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
