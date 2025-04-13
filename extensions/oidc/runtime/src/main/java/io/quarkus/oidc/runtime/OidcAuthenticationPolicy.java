package io.quarkus.oidc.runtime;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.vertx.ext.web.RoutingContext;

sealed interface OidcAuthenticationPolicy extends Consumer<SecurityIdentity>
        permits OidcAuthenticationPolicy.OAuth2StepUpAuthenticationPolicy {

    String AUTHENTICATION_POLICY_KEY = "io.quarkus.oidc.runtime.OidcAuthenticationPolicy";

    void validate(SecurityIdentity securityIdentity);

    default void accept(SecurityIdentity securityIdentity) {
        validate(securityIdentity);
    }

    final class OAuth2StepUpAuthenticationPolicy implements OidcAuthenticationPolicy {

        private static final Logger LOG = Logger.getLogger(OAuth2StepUpAuthenticationPolicy.class);
        private static final String ACR_VALUES = "acr_values";
        private static final String INSUFFICIENT_USER_AUTH_FAILURE_KEY = "io.quarkus.oidc.runtime#insufficientUserAuthFailure";
        private final String[] expectedAcrValues;

        private OAuth2StepUpAuthenticationPolicy(String[] expectedAcrValues) {
            this.expectedAcrValues = expectedAcrValues;
        }

        @Override
        public void validate(SecurityIdentity identity) {
            if (identity == null) {
                // the acr values are required, therefore authentication is required
                throw new AuthenticationFailedException(
                        "Valid token with '%s' acr claim values is required".formatted(Arrays.toString(expectedAcrValues)));
            }
            final List<String> actualAcrValues = getActualAcrValues(identity);
            if (expectedAcrValuesAreMissing(actualAcrValues)) {
                LOG.debug("Token does not contain all of required 'acr' values: " + Arrays.toString(expectedAcrValues));
                throw insufficientUserAuthException(identity, null);
            }
        }

        private boolean expectedAcrValuesAreMissing(List<String> actualAcrValues) {
            if (actualAcrValues.isEmpty()) {
                return true;
            }
            for (String expectedAcrValue : expectedAcrValues) {
                if (!actualAcrValues.contains(expectedAcrValue)) {
                    return true;
                }
            }
            return false;
        }

        private List<String> getActualAcrValues(SecurityIdentity identity) {
            final List<String> actualAcrValues;
            if (identity.getPrincipal() instanceof OidcJwtCallerPrincipal principal
                    && principal.getCredential() instanceof AccessTokenCredential) {
                try {
                    actualAcrValues = principal.getClaims().getStringListClaimValue(Claims.acr.name());
                } catch (MalformedClaimException e) {
                    throw insufficientUserAuthException(identity, e);
                }
            } else {
                AccessTokenCredential credential = OidcUtils.getTokenCredential(identity, AccessTokenCredential.class);
                if (credential == null) {
                    LOG.debug("No access token credential found");
                    throw insufficientUserAuthException(identity, null);
                }
                try {
                    actualAcrValues = new JwtConsumerBuilder()
                            .setSkipSignatureVerification()
                            .setSkipAllValidators()
                            .build().processToClaims(credential.getToken())
                            .getStringListClaimValue(Claims.acr.name());
                } catch (InvalidJwtException | MalformedClaimException e) {
                    throw insufficientUserAuthException(identity, e);
                }
            }
            return actualAcrValues;
        }

        private AuthenticationFailedException insufficientUserAuthException(SecurityIdentity identity, Exception e) {
            var authenticationFailedEx = new AuthenticationFailedException(e,
                    Map.of(ACR_VALUES, String.join(",", expectedAcrValues)));
            RoutingContext routingContext = HttpSecurityUtils.getRoutingContextAttribute(identity);
            routingContext.put(INSUFFICIENT_USER_AUTH_FAILURE_KEY, authenticationFailedEx);
            return authenticationFailedEx;
        }

        static boolean isInsufficientUserAuth(RoutingContext routingContext) {
            return routingContext.get(INSUFFICIENT_USER_AUTH_FAILURE_KEY) != null;
        }

        static String getAuthRequirementChallenge(RoutingContext context) {
            String acrValues = ((AuthenticationFailedException) context.get(INSUFFICIENT_USER_AUTH_FAILURE_KEY))
                    .getAttribute(ACR_VALUES);
            return " error=\"insufficient_user_authentication\"," +
                    " error_description=\"A different authentication level is required\"," +
                    " acr_values=\"" + acrValues + "\"";
        }

        static OidcAuthenticationPolicy create(String acrValues) {
            return new OAuth2StepUpAuthenticationPolicy(acrValues.split(","));
        }
    }

}
