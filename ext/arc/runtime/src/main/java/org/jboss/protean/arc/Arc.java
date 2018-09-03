package org.jboss.protean.arc;

/**
 *
 * @author Martin Kouba
 */
public final class Arc {

    private static final LazyValue<ArcContainerImpl> INSTANCE = new LazyValue<>(() -> new ArcContainerImpl());

    /**
     *
     * @return the container instance which loads beans using the service provider
     */
    public static ArcContainer container() {
        return INSTANCE.get();
    }

    public static void shutdown() {
        if (INSTANCE.isSet()) {
            synchronized (INSTANCE) {
                INSTANCE.get().shutdown();
                INSTANCE.clear();
            }
        }
    }

}
