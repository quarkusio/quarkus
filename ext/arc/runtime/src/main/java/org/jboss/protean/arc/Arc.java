package org.jboss.protean.arc;

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
            ArcContainerImpl container = new ArcContainerImpl();
            INSTANCE.set(container);
            container.init();
        }
        return container();
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
                if (INSTANCE.get() != null) {
                    INSTANCE.get().shutdown();
                    INSTANCE.set(null);
                    INITIALIZED.set(false);
                }
            }
        }
    }

}
