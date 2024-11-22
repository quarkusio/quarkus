package io.quarkus.websockets.next.deployment.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.websockets-next.server")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface WebSocketsServerBuildConfig {

    /**
     * Specifies the activation strategy for the CDI request context during endpoint callback invocation. By default, the
     * request context is only activated if needed, i.e. if there is a bean with the given scope, or a bean annotated
     * with a security annotation (such as {@code @RolesAllowed}), in the dependency tree of the endpoint.
     */
    @WithDefault("auto")
    ContextActivation activateRequestContext();

    /**
     * Specifies the activation strategy for the CDI session context during endpoint callback invocation. By default, the
     * session context is only activated if needed, i.e. if there is a bean with the given scope in the dependency tree of the
     * endpoint.
     */
    @WithDefault("auto")
    ContextActivation activateSessionContext();

    enum ContextActivation {
        /**
         * The context is only activated if needed.
         */
        AUTO,
        /**
         * The context is always activated.
         */
        ALWAYS
    }

}
