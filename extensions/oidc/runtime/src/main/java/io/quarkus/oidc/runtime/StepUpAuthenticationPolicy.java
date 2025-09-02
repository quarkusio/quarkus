package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.common.runtime.OidcConstants.ACR_VALUES;
import static io.quarkus.oidc.common.runtime.OidcConstants.MAX_AGE;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getAuthenticationFailureFromEvent;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getRoutingContextAttribute;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

record StepUpAuthenticationPolicy(String[] expectedAcrValues, Long maxAge) implements Consumer<TokenVerificationResult> {

    private static volatile boolean enabled = false;

    StepUpAuthenticationPolicy(String acrValues, Duration maxAge) {
        this(acrValues.split(","), maxAge == null ? null : maxAge.toSeconds());
    }

    private static final Logger LOG = Logger.getLogger(StepUpAuthenticationPolicy.class);
    private static final String AUTHENTICATION_POLICY_KEY = "io.quarkus.oidc.runtime.step-up-auth";

    @Override
    public void accept(TokenVerificationResult t) {
        JsonObject json = t.localVerificationResult != null ? t.localVerificationResult
                : new JsonObject(t.introspectionResult.getIntrospectionString());
        verifyAcr(json);
        if (maxAge != null) {
            verifyMaxAge(json);
        }
    }

    private void verifyMaxAge(JsonObject json) {
        Long authTime = json.getLong(Claims.auth_time.name());
        if (authTime == null) {
            authTime = json.getLong(Claims.iat.name());
            if (authTime != null) {
                LOG.debugf("The '%s' claim value is not available, using the '%s' claim value '%s' to verify maximum token age",
                        Claims.auth_time.name(), Claims.iat.name(), authTime);
            }
        }
        if (authTime != null) {
            final long nowSecs = System.currentTimeMillis() / 1000;
            if (nowSecs - authTime > maxAge) {
                throwAuthenticationFailedException(
                        "The token age '%d' has exceeded '%d'".formatted(authTime + maxAge, nowSecs));
            }
        } else {
            throwAuthenticationFailedException("Token has no '%s' claim".formatted(Claims.auth_time.name()));
        }
    }

    private void verifyAcr(JsonObject json) {
        JsonArray acr = json.getJsonArray(OidcConstants.ACR);
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
        throwAuthenticationFailedException(exceptionMessage, expectedAcrValues, maxAge);
    }

    private static void throwAuthenticationFailedException(String exceptionMessage, String[] expectedAcrValues, Long maxAge) {
        final Map<String, Object> failureContext;
        if (maxAge == null) {
            failureContext = Map.of(ACR_VALUES, String.join(",", expectedAcrValues));
        } else {
            failureContext = Map.of(ACR_VALUES, String.join(",", expectedAcrValues), MAX_AGE, Long.toString(maxAge));
        }
        throw new AuthenticationFailedException(exceptionMessage, failureContext);
    }

    static void throwAuthenticationFailedException(String exceptionMessage, Set<String> expectedAcrValues) {
        throwAuthenticationFailedException(exceptionMessage, expectedAcrValues.toArray(new String[] {}), null);
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

    static void markAsEnabled() {
        enabled = true;
    }

    static boolean isEnabled() {
        return enabled;
    }
}
