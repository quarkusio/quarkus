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

    /**
     * If enabled, the WebSocket opening handshake headers are enhanced with the 'Sec-WebSocket-Protocol' sub-protocol
     * that match format 'quarkus-http-upgrade#header-name#header-value'. If the WebSocket client interface does not support
     * setting headers to the WebSocket opening handshake, this is a way how to set authorization header required to
     * authenticate user. The 'quarkus-http-upgrade' sub-protocol is removed and server selects from the sub-protocol one
     * that is supported (don't forget to configure the 'quarkus.websockets-next.server.supported-subprotocols' property).
     * <b>IMPORTANT: We strongly recommend to only enable this feature if the HTTP connection is encrypted via TLS,
     * CORS origin check is enabled and custom WebSocket ticket system is in place.
     * Please see the Quarkus WebSockets Next reference for more information.</b>
     */
    @WithDefault("false")
    boolean propagateSubprotocolHeaders();

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
