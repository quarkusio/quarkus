package io.quarkus.websockets.next;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.websockets-next.server")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface WebSocketsServerBuildConfig {

    /**
     * Specifies whether to activate the CDI request context when an endpoint callback is invoked. By default, the request
     * context is only activated if needed.
     */
    @WithDefault("auto")
    RequestContextActivation activateRequestContext();

    enum RequestContextActivation {
        /**
         * The request context is only activated if needed, i.e. if there is a request scoped bean , or a bean annotated
         * with a security annotation (such as {@code @RolesAllowed}) in the dependency tree of the endpoint.
         */
        AUTO,
        /**
         * The request context is always activated.
         */
        ALWAYS
    }

}
