package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.AuthenticationContext.NO_MAX_AGE;
import static io.quarkus.oidc.common.runtime.OidcConstants.ACR_VALUES;
import static io.quarkus.oidc.common.runtime.OidcConstants.MAX_AGE;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getAuthenticationFailureFromEvent;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getRoutingContextAttribute;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

record StepUpAuthenticationPolicy(String[] expectedAcrValues, long maxAge) implements Consumer<TokenVerificationResult> {

    StepUpAuthenticationPolicy(String acrValues, String maxAge) {
        this(acrValues.isEmpty() ? null : acrValues.split(","), Long.parseLong(maxAge));
    }

    private static final Logger LOG = Logger.getLogger(StepUpAuthenticationPolicy.class);
    private static final String AUTHENTICATION_POLICY_KEY = "io.quarkus.oidc.runtime.step-up-auth";

    @Override
    public void accept(TokenVerificationResult t) {
        JsonObject json = t.localVerificationResult != null ? t.localVerificationResult
                : new JsonObject(t.introspectionResult.getIntrospectionString());
        if (expectedAcrValues != null) {
            verifyAcr(json);
        }
        if (maxAge != NO_MAX_AGE) {
            verifyMaxAge(json);
        }
    }

    private void verifyMaxAge(JsonObject json) {
        Long authTime = json.getLong(Claims.auth_time.name());
        if (authTime != null) {
            final long nowSecs = System.currentTimeMillis() / 1000;
            final long expiresAtSecs = authTime + maxAge;
            if (expiresAtSecs <= nowSecs) {
                throwAuthenticationFailedException("Token issued at " + authTime + " is expired, because max age was set to "
                        + maxAge + " using the '@AuthenticationContext' annotation and now is " + nowSecs);
            }
        } else {
            throwAuthenticationFailedException("Token has no '%s' claim".formatted(Claims.auth_time.name()));
        }
    }

    private void verifyAcr(JsonObject json) {
        JsonArray acr = json.getJsonArray(Claims.acr.name());
        if (acr != null && !acr.isEmpty()) {
            boolean acrFound = true;
            for (String expectedAcrValue : expectedAcrValues) {
                if (!acr.contains(expectedAcrValue)) {
                    LOG.debug("Acr value " + expectedAcrValue + " is required but not found in token 'acr' claim: " + acr);
                    acrFound = false;
                    break;
                }
            }
            if (acrFound) {
                return;
            }
        }

        final String message = "Valid token with '%s' acr claim values is required"
                .formatted(Arrays.toString(expectedAcrValues));
        throwAuthenticationFailedException(message);
    }

    private void throwAuthenticationFailedException(String exceptionMessage) {
        final Map<String, Object> failureContext;
        if (maxAge == NO_MAX_AGE) {
            failureContext = Map.of(ACR_VALUES, String.join(",", expectedAcrValues));
        } else {
            if (expectedAcrValues != null) {
                failureContext = Map.of(ACR_VALUES, String.join(",", expectedAcrValues), MAX_AGE, Long.toString(maxAge));
            } else {
                failureContext = Map.of(MAX_AGE, Long.toString(maxAge));
            }
        }
        throw new AuthenticationFailedException(exceptionMessage, failureContext);
    }

    void storeSelfOnContext(RoutingContext routingContext) {
        routingContext.put(AUTHENTICATION_POLICY_KEY, this);
    }

    static StepUpAuthenticationPolicy getFromRequest(TokenAuthenticationRequest request) {
        RoutingContext routingContext = getRoutingContextAttribute(request);
        return routingContext != null ? routingContext.get(AUTHENTICATION_POLICY_KEY) : null;
    }

    static boolean isInsufficientUserAuthException(RoutingContext routingContext) {
        final AuthenticationFailedException authFailure = getAuthenticationFailureFromEvent(routingContext);
        return isInsufficientUserAuthException(authFailure);
    }

    static String getAuthRequirementChallenge(RoutingContext context) {
        final AuthenticationFailedException authFailure = getAuthenticationFailureFromEvent(context);
        if (isInsufficientUserAuthException(authFailure)) {
            StringBuilder challengeBuilder = new StringBuilder(" error=\"insufficient_user_authentication\"," +
                    " error_description=\"A different authentication level is required\"");
            if (authFailure.getAttribute(ACR_VALUES) != null) {
                challengeBuilder.append(", ").append(ACR_VALUES).append("=\"")
                        .append((String) authFailure.getAttribute(ACR_VALUES))
                        .append("\"");
            }
            if (authFailure.getAttribute(MAX_AGE) != null) {
                challengeBuilder.append(", ").append(MAX_AGE).append("=\"")
                        .append((String) authFailure.getAttribute(MAX_AGE))
                        .append("\"");
            }
            return challengeBuilder.toString();
        }
        return null;
    }

    private static boolean isInsufficientUserAuthException(AuthenticationFailedException authFailure) {
        return authFailure != null
                && (authFailure.getAttribute(ACR_VALUES) != null || authFailure.getAttribute(MAX_AGE) != null);
    }
}
