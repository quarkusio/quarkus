package io.quarkus.arc;

import java.lang.StackWalker.StackFrame;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.arc.impl.ArcContainerImpl;

/**
 * Provides access to the ArC container.
 */
public final class Arc {

    private static final AtomicReference<ArcContainerImpl> INSTANCE = new AtomicReference<>();

    /**
     * If this system property is set then log a warning with diagnostic information when {@link Arc#container()} returns
     * {@code null}.
     *
     * @see #container()
     */
    private static final String LOG_NO_CONTAINER = "quarkus.arc.log-no-container";

    private static final Logger LOG = Logger.getLogger(Arc.class);

    /**
     * Initializes {@link ArcContainer} with default settings.
     * This is equal to using {@code Arc#initialize(ArcInitConfig.INSTANCE)}
     *
     * @return the container instance with default configuration
     */
    public static ArcContainer initialize() {
        return initialize(ArcInitConfig.DEFAULT);
    }

    /**
     *
     * @param config
     * @return the container instance
     * @see #initialize()
     */
    public static ArcContainer initialize(ArcInitConfig config) {
        ArcContainerImpl container = INSTANCE.get();
        if (container == null) {
            synchronized (INSTANCE) {
                container = INSTANCE.get();
                if (container == null) {
                    // Set the container instance first because Arc.container() can be used within ArcContainerImpl.init()
                    container = new ArcContainerImpl(config.getCurrentContextFactory(), config.isStrictCompatibility(),
                            config.isTestMode());
                    INSTANCE.set(container);
                    container.init();
                }
            }
        }
        return container;
    }

    public static void setExecutor(ExecutorService executor) {
        ArcContainerImpl container = INSTANCE.get();
        if (container == null) {
            throw containerNotInitialized();
        }
        container.setExecutor(executor);
    }

    /**
     *
     * @return the container instance or {@code null} if the container is not initialized
     */
    public static ArcContainer container() {
        ArcContainer container = INSTANCE.get();
        if (container == null && System.getProperty(LOG_NO_CONTAINER) != null) {
            LOG.warn(gatherDiagnosticInfo());
        }
        return container;
    }

    /**
     * @return the container instance
     * @throws IllegalStateException if the container is not initialized
     */
    public static ArcContainer requireContainer() {
        ArcContainer container = INSTANCE.get();
        if (container == null) {
            throw containerNotInitialized();
        }
        return container;
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

    private static IllegalStateException containerNotInitialized() {
        String msg = "ArC container not initialized: the container is not started, already shut down, or a wrong class loader was used to load the io.quarkus.arc.Arc class\n"
                + "\t- CL:  %s\n"
                + "\t- TCCL: %s";
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        ClassLoader cl = Arc.class.getClassLoader();
        return new IllegalStateException(msg.formatted(cl, tccl));
    }

    private static String gatherDiagnosticInfo() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        ClassLoader cl = Arc.class.getClassLoader();
        String msg = "\n"
                + "==============================\n"
                + "ArC: container not initialized\n"
                + "------------------------------\n"
                + "The container is not started, already shut down, or a wrong class loader was used to load the io.quarkus.arc.Arc class.\n"
                + "\n"
                + "CL:   %1$s\n"
                + "TCCL: %2$s\n"
                + "Stack:\n\t%3$s"
                + "\n"
                + "===================================\n";
        StackWalker walker = StackWalker.getInstance();
        return msg.formatted(cl, tccl, walker.walk(Arc::collectStack));
    }

    private static String collectStack(Stream<StackFrame> stream) {
        return stream
                .skip(1)
                .map(Object::toString)
                .collect(Collectors.joining("\n\t"));
    }

}
