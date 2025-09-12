package io.quarkus.vertx.http.runtime.security;

import java.util.Map;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.spi.runtime.AbstractSecurityEvent;
import io.vertx.ext.web.RoutingContext;

public final class FormAuthenticationEvent extends AbstractSecurityEvent {

    public static final String FORM_CONTEXT = "io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent#CONTEXT";
    /**
     * Form event attribute added when Quarkus received a request to generate one-time authentication token.
     */
    public static final String REQUEST_USERNAME = "io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent#REQUEST_USERNAME";
    /**
     * Form event attribute added when Quarkus received a request to generate one-time authentication token,
     * but authentication failed when Quarkus tried to retrieve the user with requested username.
     */
    public static final String AUTHENTICATION_FAILURE = "io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent#AUTHENTICATION_FAILURE";
    public static final String CREDENTIALS_TYPE = "io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent#CREDENTIALS_TYPE";
    public static final String USERNAME_PASSWORD_REQUEST = "USERNAME_AND_PASSWORD";
    public static final String ONE_TIME_AUTH_TOKEN_REQUEST = "ONE_TIME_AUTHENTICATION_TOKEN";

    public enum FormEventType {
        /**
         * Event fired when a user was successfully authenticated with a call to the Form mechanism POST location.
         */
        FORM_LOGIN,
        /**
         * Event fired when Quarkus received one-time authentication token request.
         */
        ONE_TIME_AUTH_TOKEN_REQUESTED
    }

    private FormAuthenticationEvent(SecurityIdentity securityIdentity, Map<String, Object> eventProperties) {
        super(securityIdentity, eventProperties);
    }

    static FormAuthenticationEvent createEmptyLoginEvent() {
        return createLoginEvent(null, USERNAME_PASSWORD_REQUEST);
    }

    static FormAuthenticationEvent createLoginEvent(SecurityIdentity identity, AuthenticationRequest authenticationRequest) {
        final String credentialsType;
        if (authenticationRequest instanceof UsernamePasswordAuthenticationRequest) {
            credentialsType = USERNAME_PASSWORD_REQUEST;
        } else if (authenticationRequest instanceof TrustedAuthenticationRequest) {
            credentialsType = ONE_TIME_AUTH_TOKEN_REQUEST;
        } else {
            throw new IllegalStateException("Unknown credentials type " + authenticationRequest.getClass());
        }
        return createLoginEvent(identity, credentialsType);
    }

    private static FormAuthenticationEvent createLoginEvent(SecurityIdentity identity, String credentialsType) {
        return new FormAuthenticationEvent(identity, Map.of(FORM_CONTEXT, FormEventType.FORM_LOGIN.toString(),
                CREDENTIALS_TYPE, credentialsType));
    }

    static FormAuthenticationEvent createOneTimeAuthTokenRequestEvent(SecurityIdentity identity, String requestedUsername,
            RoutingContext event) {
        return new FormAuthenticationEvent(identity, Map.of(FORM_CONTEXT,
                FormEventType.ONE_TIME_AUTH_TOKEN_REQUESTED.toString(), REQUEST_USERNAME, requestedUsername,
                HttpSecurityUtils.ROUTING_CONTEXT_ATTRIBUTE, event));
    }

    static FormAuthenticationEvent createOneTimeAuthTokenRequestEvent(Throwable authenticationFailure, String requestedUsername,
            RoutingContext event) {
        return new FormAuthenticationEvent(null, Map.of(FORM_CONTEXT,
                FormEventType.ONE_TIME_AUTH_TOKEN_REQUESTED.toString(), AUTHENTICATION_FAILURE, authenticationFailure,
                REQUEST_USERNAME, requestedUsername, HttpSecurityUtils.ROUTING_CONTEXT_ATTRIBUTE, event));
    }
}
