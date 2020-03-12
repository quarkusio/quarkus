package io.quarkus.arc;

import io.quarkus.arc.impl.ArcContainerImpl;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Martin Kouba
 */
public final class Arc {

    private static final AtomicReference<ArcContainerImpl> INSTANCE = new AtomicReference<>();
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    public static ArcContainer initialize() {
        if (INITIALIZED.compareAndSet(false, true)) {
            try {
                ArcContainerImpl container = new ArcContainerImpl();
                INSTANCE.set(container);
                container.init();
                return container;
            } catch (Throwable t) {
                INITIALIZED.set(false);
                throw new RuntimeException("Failed to initialize Arc", t);
            }
        }
        return container();
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
        if (INSTANCE.get() != null) {
            synchronized (INSTANCE) {
                ArcContainerImpl container = INSTANCE.get();
                if (container != null) {
                    container.shutdown();
                    INSTANCE.set(null);
                    INITIALIZED.set(false);
                }
            }
        }
    }

}
