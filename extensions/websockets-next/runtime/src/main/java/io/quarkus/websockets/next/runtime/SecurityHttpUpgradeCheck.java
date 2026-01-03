package io.quarkus.websockets.next.runtime;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;

import java.util.List;
import java.util.Map;

import io.quarkus.arc.ClientProxy;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;

public final class SecurityHttpUpgradeCheck implements HttpUpgradeCheck {

    public static final int BEAN_PRIORITY = Integer.MAX_VALUE - 100;
    public static final String SECURED_ENDPOINT_ID_KEY = SecurityHttpUpgradeCheck.class.getName() + ".ENDPOINT_ID";
    public static final String HTTP_REQUEST_KEY = SecurityHttpUpgradeCheck.class.getName() + ".HTTP_REQUEST";

    private final String redirectUrl;
    private final Map<String, SecurityCheck> endpointToCheck;
    private final Map<String, HttpSecurityPolicy> endpointToPolicy;
    private final SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> securityEventHelper;
    private final HttpSecurityPolicy.AuthorizationRequestContext authorizationRequestContext;

    SecurityHttpUpgradeCheck(String redirectUrl, Map<String, SecurityCheck> endpointToCheck,
            SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> securityEventHelper,
            Map<String, HttpSecurityPolicy> endpointToPolicy,
            HttpSecurityPolicy.AuthorizationRequestContext authorizationRequestContext) {
        this.redirectUrl = redirectUrl;
        this.endpointToCheck = Map.copyOf(endpointToCheck);
        this.securityEventHelper = securityEventHelper;
        this.authorizationRequestContext = authorizationRequestContext;
        this.endpointToPolicy = Map.copyOf(endpointToPolicy);
    }

    @Override
    public Uni<CheckResult> perform(HttpUpgradeContext context) {
        final SecurityCheck securityCheck = endpointToCheck.get(context.endpointId());
        final HttpSecurityPolicy httpSecurityPolicy = endpointToPolicy.get(context.endpointId());

        if (httpSecurityPolicy != null) {
            if (securityCheck != null) {
                // ATM this must be a validation failure during the build time, hence we should never get here
                // because for now, security annotations cannot be combined anywhere in Quarkus
                return CheckResult.rejectUpgrade(403);
            } else if (context instanceof HttpUpgradeContextImpl impl) {
                return httpSecurityPolicy
                        .checkPermission(impl.routingContext(), context.securityIdentity(), authorizationRequestContext)
                        .onItemOrFailure().transform((checkResult, throwable) -> {
                            if (checkResult == null) {
                                return rejectUpgrade(throwable, getSecurityIdentity(impl),
                                        getHttpSecurityPolicyClass(httpSecurityPolicy), context);
                            }

                            final SecurityIdentity securityIdentity;
                            if (checkResult.getAugmentedIdentity() != null) {
                                securityIdentity = checkResult.getAugmentedIdentity();
                                QuarkusHttpUser.setIdentity(securityIdentity, impl.routingContext());
                            } else {
                                securityIdentity = getSecurityIdentity(impl);
                            }

                            if (checkResult.isPermitted()) {
                                return permitUpgrade(securityIdentity, getHttpSecurityPolicyClass(httpSecurityPolicy), context);
                            } else {
                                return rejectUpgrade(throwable, securityIdentity,
                                        getHttpSecurityPolicyClass(httpSecurityPolicy), context);
                            }
                        });
            } else {
                // there must be only one implementation of the upgrade contexts, otherwise tests will fail
                return CheckResult.rejectUpgrade(403);
            }
        } else {
            if (securityCheck == null) {
                // illegal state - this check must be applied on secured endpoints only
                return CheckResult.rejectUpgrade(403);
            } else {
                return context.securityIdentity().chain(identity -> securityCheck
                        .nonBlockingApply(identity, (MethodDescription) null, null)
                        .replaceWith(() -> permitUpgrade(identity, securityCheck, context))
                        .onFailure(SecurityException.class)
                        .recoverWithItem(t -> rejectUpgrade(t, identity, securityCheck, context)));
            }
        }
    }

    @Override
    public boolean appliesTo(String endpointId) {
        return endpointToCheck.containsKey(endpointId) || endpointToPolicy.containsKey(endpointId);
    }

    private CheckResult permitUpgrade(SecurityIdentity identity, Object authorizationCheck, HttpUpgradeContext context) {
        if (securityEventHelper.fireEventOnSuccess()) {
            String authorizationContext = authorizationCheck.getClass().getName();
            AuthorizationSuccessEvent successEvent = new AuthorizationSuccessEvent(identity, authorizationContext,
                    Map.of(SECURED_ENDPOINT_ID_KEY, context.endpointId(), HTTP_REQUEST_KEY, context.httpRequest()));
            securityEventHelper.fireSuccessEvent(successEvent);
        }
        return CheckResult.permitUpgradeSync();
    }

    private CheckResult rejectUpgrade(Throwable throwable, SecurityIdentity identity, Object authorizationCheck,
            HttpUpgradeContext context) {
        if (securityEventHelper.fireEventOnFailure()) {
            String authorizationContext = authorizationCheck.getClass().getName();
            AuthorizationFailureEvent failureEvent = new AuthorizationFailureEvent(identity, throwable, authorizationContext,
                    Map.of(SECURED_ENDPOINT_ID_KEY, context.endpointId(), HTTP_REQUEST_KEY, context.httpRequest()));
            securityEventHelper.fireFailureEvent(failureEvent);
        }
        if (redirectUrl != null) {
            return CheckResult.rejectUpgradeSync(302,
                    Map.of(LOCATION.toString(), List.of(redirectUrl),
                            CACHE_CONTROL.toString(), List.of("no-store")));
        } else if (throwable instanceof ForbiddenException || (identity != null && !identity.isAnonymous())) {
            return CheckResult.rejectUpgradeSync(403);
        } else {
            return CheckResult.rejectUpgradeSync(401);
        }
    }

    private static SecurityIdentity getSecurityIdentity(HttpUpgradeContextImpl impl) {
        if (impl.routingContext().user() instanceof QuarkusHttpUser user) {
            return user.getSecurityIdentity();
        }
        return null;
    }

    private static String getHttpSecurityPolicyClass(HttpSecurityPolicy httpSecurityPolicy) {
        return ClientProxy.unwrap(httpSecurityPolicy).getClass().getName();
    }
}
