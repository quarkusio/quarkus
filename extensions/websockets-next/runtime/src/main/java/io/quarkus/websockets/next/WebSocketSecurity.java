package io.quarkus.websockets.next;

import java.util.concurrent.CompletionStage;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.common.annotation.Experimental;

/**
 * A CDI bean that allows to update {@link SecurityIdentity} before previous a bearer access token expires.
 */
@Experimental("Support for the SecurityIdentity update is experimental")
public interface WebSocketSecurity {

    /**
     * Authenticate with the bearer access token and update current {@link SecurityIdentity} attached to the WebSocket
     * server connection. The update is only possible if the current {@link SecurityIdentity} and the bearer access
     * token represent the same authenticated user.
     *
     * @param accessToken bearer access token
     * @return authentication result
     */
    CompletionStage<SecurityIdentity> updateSecurityIdentity(String accessToken);

}
