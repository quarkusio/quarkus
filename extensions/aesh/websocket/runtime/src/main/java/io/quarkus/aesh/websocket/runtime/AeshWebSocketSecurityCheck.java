package io.quarkus.aesh.websocket.runtime;

import java.util.List;

import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;

/**
 * An {@link HttpUpgradeCheck} that enforces authentication and role-based access
 * on the aesh WebSocket terminal endpoint.
 */
public class AeshWebSocketSecurityCheck implements HttpUpgradeCheck {

    private final List<String> rolesAllowed;
    private final boolean requireAuthenticated;
    private final String endpointId;

    public AeshWebSocketSecurityCheck(List<String> rolesAllowed, boolean requireAuthenticated) {
        this.rolesAllowed = rolesAllowed;
        this.requireAuthenticated = requireAuthenticated;
        this.endpointId = AeshWebSocketEndpoint.class.getName();
    }

    @Override
    public Uni<CheckResult> perform(HttpUpgradeContext context) {
        return context.securityIdentity().map(identity -> {
            if (identity == null || identity.isAnonymous()) {
                return CheckResult.rejectUpgradeSync(401);
            }
            if (rolesAllowed != null && !rolesAllowed.isEmpty()) {
                boolean hasRole = rolesAllowed.stream().anyMatch(identity::hasRole);
                if (!hasRole) {
                    return CheckResult.rejectUpgradeSync(403);
                }
            }
            return CheckResult.permitUpgradeSync();
        });
    }

    @Override
    public boolean appliesTo(String endpointId) {
        return this.endpointId.equals(endpointId);
    }
}
