package io.quarkus.smallrye.graphql.runtime.exception;

import java.util.Collections;
import java.util.Map;

/**
 * An exception which is thrown when a security identity is already defined, but the connection init payload tries to
 * change the connections identity. This is not allowed.
 */
public class SmallRyeAuthSecurityIdentityAlreadyAssignedException extends Exception {

    private final Map<String, Object> connectionInitPayload;

    public SmallRyeAuthSecurityIdentityAlreadyAssignedException(Map<String, Object> connectionInitPayload) {
        super("Security identity already assigned to this WebSocket.");
        this.connectionInitPayload = Collections.unmodifiableMap(connectionInitPayload);
    }

    public Map<String, Object> getConnectionInitPayload() {
        return connectionInitPayload;
    }
}
