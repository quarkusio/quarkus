package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.virtual.threads.DefaultExecutionMode;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.rest")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ResteasyReactiveServerConfig {

    /**
     * Set this to define the application path that serves as the base URI for all
     * JAX-RS resource URIs provided by {@code @Path} annotations when there are no
     * {@code @ApplicationPath} annotations defined on {@code Application} classes.
     * <p>
     * This value is always resolved relative to {@code quarkus.http.root-path}.
     */
    Optional<String> path();

    /**
     * The execution mode used for blocking endpoints without an explicit {@code @Blocking},
     * {@code @NonBlocking} or {@code @RunOnVirtualThread} annotation.
     * <p>
     * When set to {@code virtual-thread}, such endpoints are executed on virtual threads
     * instead of worker threads. Endpoints considered non-blocking, for instance because
     * they return a reactive type, keep running on the event loop, and explicit annotations
     * on the endpoint method, the resource class or the {@code Application} class always
     * take precedence.
     * <p>
     * Before changing this, make sure the application does not suffer from thread pinning
     * (mostly resolved since JDK 24) or carrier thread monopolization. See the virtual
     * threads guide for more details on how to monitor this with JFR events.
     */
    @WithDefault("worker")
    DefaultExecutionMode defaultBlockingExecutionMode();
}
