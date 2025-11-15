package io.quarkus.qute.runtime.debug;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.debug.adapter.RegisterDebugServerAdapter;
import io.quarkus.qute.runtime.QuteConfig;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.common.annotation.Experimental;

/**
 * Observes the creation of Qute engines and attaches the Qute debugger in development mode.
 *
 * <p>
 * When a new {@link io.quarkus.qute.EngineBuilder} is observed and the application is started with "-DquteDebugPort"
 * and running in
 * {@link io.quarkus.runtime.LaunchMode#DEVELOPMENT development mode} with
 * {@code quarkus.qute.debug.enabled=true}, this observer:
 * <ul>
 * <li>Enables template tracing on the engine (required for the debugger).</li>
 * <li>Registers a {@link RegisterDebugServerAdapter} to allow DAP clients to connect.</li>
 * </ul>
 *
 * <p>
 * The {@link #cleanup()} method ensures that the debugger is properly reset when the application shuts down.
 */
@Singleton
@Experimental("This observer is experimental and may change in the future")
public class DebugQuteEngineObserver {

    private final static RegisterDebugServerAdapter registrar = new RegisterDebugServerAdapter();

    /**
     * Configures the engine with tracing and debugger support if debugging is enabled.
     *
     * @param builder the Qute engine builder being observed
     * @param config the Qute configuration
     */
    void configureEngine(@Observes EngineBuilder builder, QuteConfig config) {
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT && config.debug().enabled() && registrar.getPort() != null) {
            // - in dev mode only
            // - quarkus.qute.debug.enabled=true
            // - Quarkus application is started with -DquteDebugPort
            // --> enable the Qute debugger.
            builder.enableTracing(true);
            builder.addEngineListener(registrar);
        }
    }

    /**
     * Cleans up the debugger on shutdown by resetting the registered debug server adapter.
     */
    @PreDestroy
    void cleanup() {
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            registrar.reset();
        }
    }
}
