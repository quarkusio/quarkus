package io.quarkus.websockets.next.runtime;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;

import java.util.List;
import java.util.Map;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;

public final class SecurityHttpUpgradeCheck implements HttpUpgradeCheck {

    public static final int BEAN_PRIORITY = Integer.MAX_VALUE - 100;
    public static final String SECURED_ENDPOINT_ID_KEY = SecurityHttpUpgradeCheck.class.getName() + ".ENDPOINT_ID";
    public static final String HTTP_REQUEST_KEY = SecurityHttpUpgradeCheck.class.getName() + ".HTTP_REQUEST";

    private final String redirectUrl;
    private final Map<String, SecurityCheck> endpointToCheck;
    private final SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> securityEventHelper;

    SecurityHttpUpgradeCheck(String redirectUrl, Map<String, SecurityCheck> endpointToCheck,
            SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> securityEventHelper) {
        this.redirectUrl = redirectUrl;
        this.endpointToCheck = Map.copyOf(endpointToCheck);
        this.securityEventHelper = securityEventHelper;
    }

    @Override
    public Uni<CheckResult> perform(HttpUpgradeContext context) {
        final SecurityCheck securityCheck = endpointToCheck.get(context.endpointId());
        return context.securityIdentity().chain(identity -> securityCheck
                .nonBlockingApply(identity, (MethodDescription) null, null)
                .replaceWith(() -> permitUpgrade(identity, securityCheck, context))
                .onFailure(SecurityException.class)
                .recoverWithItem(t -> rejectUpgrade(t, identity, securityCheck, context)));
    }

    @Override
    public boolean appliesTo(String endpointId) {
        return endpointToCheck.containsKey(endpointId);
    }

    private CheckResult permitUpgrade(SecurityIdentity identity, SecurityCheck securityCheck, HttpUpgradeContext context) {
        if (securityEventHelper.fireEventOnSuccess()) {
            String authorizationContext = securityCheck.getClass().getName();
            AuthorizationSuccessEvent successEvent = new AuthorizationSuccessEvent(identity, authorizationContext,
                    Map.of(SECURED_ENDPOINT_ID_KEY, context.endpointId(), HTTP_REQUEST_KEY, context.httpRequest()));
            securityEventHelper.fireSuccessEvent(successEvent);
        }
        return CheckResult.permitUpgradeSync();
    }

    private CheckResult rejectUpgrade(Throwable throwable, SecurityIdentity identity, SecurityCheck securityCheck,
            HttpUpgradeContext context) {
        if (securityEventHelper.fireEventOnFailure()) {
            String authorizationContext = securityCheck.getClass().getName();
            AuthorizationFailureEvent failureEvent = new AuthorizationFailureEvent(identity, throwable, authorizationContext,
                    Map.of(SECURED_ENDPOINT_ID_KEY, context.endpointId(), HTTP_REQUEST_KEY, context.httpRequest()));
            securityEventHelper.fireFailureEvent(failureEvent);
        }
        if (redirectUrl != null) {
            return CheckResult.rejectUpgradeSync(302,
                    Map.of(LOCATION.toString(), List.of(redirectUrl),
                            CACHE_CONTROL.toString(), List.of("no-store")));
        } else if (throwable instanceof ForbiddenException) {
            return CheckResult.rejectUpgradeSync(403);
        } else {
            return CheckResult.rejectUpgradeSync(401);
        }
    }

}
