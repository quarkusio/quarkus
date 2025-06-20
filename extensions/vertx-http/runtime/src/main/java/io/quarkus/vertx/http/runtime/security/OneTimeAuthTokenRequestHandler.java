package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.fire;
import static io.quarkus.vertx.http.runtime.FormAuthRuntimeConfig.CookieSameSite.STRICT;
import static io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent.*;
import static io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism.sendRedirect;
import static io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism.startWithSlash;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.vertx.http.runtime.FormAuthRuntimeConfig;
import io.quarkus.vertx.http.security.token.OneTimeAuthenticationTokenSender;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

final class OneTimeAuthTokenRequestHandler {

    private static final Logger LOG = Logger.getLogger(OneTimeAuthTokenRequestHandler.class);
    private static final char PRINCIPAL_TO_TOKEN_SEPARATOR = '-';
    private final Event<FormAuthenticationEvent> formAuthEvent;
    private final String onTokenGeneratedRedirectPath;
    private final OneTimeAuthenticationTokenSender tokenSender;
    private final PersistentLoginManager loginManager;

    private OneTimeAuthTokenRequestHandler(Event<FormAuthenticationEvent> formAuthEvent, PersistentLoginManager loginManager,
            String onTokenGeneratedRedirectPath, OneTimeAuthenticationTokenSender tokenSender) {
        this.formAuthEvent = formAuthEvent;
        this.onTokenGeneratedRedirectPath = onTokenGeneratedRedirectPath;
        this.tokenSender = tokenSender;
        this.loginManager = loginManager;
    }

    void handleTokenRequest(SecurityIdentity identity, RoutingContext routingContext, String userPrincipal) {
        handleTokenRequest(Uni.createFrom().item(identity), routingContext, userPrincipal);
    }

    String findUserPrincipalByToken(RoutingContext routingContext, String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        TokenAndPrincipal storedRequest = getStoredUserPrincipalAndToken(routingContext);
        if (storedRequest == null) {
            LOG.debugf("Sent token not found in request cookie, cannot compare it with token '%s'", token);
            return null;
        }
        String tokenHash = HashUtil.sha512(token);
        if (tokenHash.equals(storedRequest.token)) {
            LOG.debug("Provided token matches sent token");
            return storedRequest.principal;
        }
        LOG.debugf("Provided token '%s' does not match sent token", token);
        return null;
    }

    private void handleTokenRequest(Uni<SecurityIdentity> identityUni, RoutingContext routingContext, String userPrincipal) {
        sendOneTimeAuthToken(identityUni, routingContext, userPrincipal);
        if (onTokenGeneratedRedirectPath != null) {
            sendRedirect(routingContext, onTokenGeneratedRedirectPath);
        } else {
            routingContext.response().setStatusCode(204).end();
        }
    }

    private void sendOneTimeAuthToken(Uni<SecurityIdentity> identityUni, RoutingContext event, String username) {
        // this must be async, because we don't want response time to indicate if the username is recognized
        Uni.createFrom()
                .deferred(new Supplier<Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> get() {
                        return identityUni;
                    }
                })
                .flatMap(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<? extends SecurityIdentity> apply(SecurityIdentity identity) {
                        if (identity != null && !identity.isAnonymous()) {
                            LOG.debugf("Received one-time authentication token request for user '%s'", username);
                            return sendAndStoreOneTimeAuthToken(identity, generateOneTimeAuthToken(identity), event);
                        } else {
                            // identity provider should just fail, incorrect credentials must lead to auth failure
                            return Uni.createFrom()
                                    .failure(new AuthenticationFailedException("Failed to authenticate user " + username));
                        }
                    }
                })
                .subscribe().with(new Consumer<SecurityIdentity>() {
                    @Override
                    public void accept(SecurityIdentity identity) {
                        if (formAuthEvent != null) {
                            fire(formAuthEvent,
                                    FormAuthenticationEvent.createOneTimeAuthTokenRequestEvent(identity, username, event));
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable failure) {
                        if (formAuthEvent != null) {
                            fire(formAuthEvent, createOneTimeAuthTokenRequestEvent(failure, username, event));
                        }
                        LOG.debug("Request to send one-time token authentication failed for username '" + username + "'",
                                failure);
                    }
                });
    }

    private Uni<SecurityIdentity> sendAndStoreOneTimeAuthToken(SecurityIdentity identity, PasswordCredential oneTimeAuthToken,
            RoutingContext event) {
        return tokenSender
                .send(identity, oneTimeAuthToken)
                .invoke(new Runnable() {
                    @Override
                    public void run() {
                        store(identity, oneTimeAuthToken, event);
                    }
                })
                .invoke(new Runnable() {
                    @Override
                    public void run() {
                        Arrays.fill(oneTimeAuthToken.getPassword(), 'Q');
                    }
                })
                .replaceWith(identity);
    }

    private void store(SecurityIdentity securityIdentity, PasswordCredential tokenCredential, RoutingContext event) {
        String principalName = securityIdentity.getPrincipal().getName();
        if (principalName.isBlank()) {
            throw new IllegalArgumentException("Principal name cannot be blank");
        }
        String tokenHash = HashUtil.sha512(new String(tokenCredential.getPassword()).getBytes(UTF_8));
        // what do we store? we store token hash, we store principal name, and ... UUID I guess ?
        String cookieValue = principalName + PRINCIPAL_TO_TOKEN_SEPARATOR + tokenHash;
        loginManager.save(cookieValue, event);
    }

    static OneTimeAuthTokenRequestHandler of(FormAuthRuntimeConfig formConfig, BeanManager beanManager,
            boolean securityEventsEnabled, String key) {
        var formAuthEvent = createFormAuthEvent(beanManager, securityEventsEnabled);
        var tokenConfig = formConfig.authenticationToken();
        var tokenSender = Arc.container().select(OneTimeAuthenticationTokenSender.class).get();
        var loginManager = new PersistentLoginManager(key, tokenConfig.cookieName(), tokenConfig.expiresIn().toMillis(),
                -1, true, STRICT.name(), formConfig.cookiePath().orElse(null), tokenConfig.expiresIn().getSeconds());
        return new OneTimeAuthTokenRequestHandler(formAuthEvent, loginManager,
                startWithSlash(tokenConfig.requestRedirectPath().orElse(null)), tokenSender);
    }

    private TokenAndPrincipal getStoredUserPrincipalAndToken(RoutingContext event) {
        var cookieValue = loginManager.getAndRemoveCookie(event);
        if (cookieValue != null) {
            return new TokenAndPrincipal(cookieValue);
        }
        return null;
    }

    private static PasswordCredential generateOneTimeAuthToken(SecurityIdentity identity) {
        // one is added to make sure that one-time authentication token is unique per user (only one token is allowed per user)
        return new PasswordCredential((HashUtil.sha512(identity.getPrincipal().getName()) + UUID.randomUUID()).toCharArray());
    }

    private static Event<FormAuthenticationEvent> createFormAuthEvent(BeanManager beanManager, boolean securityEventsEnabled) {
        boolean isFormAuthEventObserver = SecurityEventHelper.isEventObserved(createEmptyLoginEvent(), beanManager,
                securityEventsEnabled);
        return isFormAuthEventObserver ? beanManager.getEvent().select(FormAuthenticationEvent.class) : null;
    }

    private record TokenAndPrincipal(String token, String principal) {

        private TokenAndPrincipal(String cookieValue) {
            this(getToken(cookieValue), getPrincipal(cookieValue));
        }

        private static String getPrincipal(String cookieValue) {
            return cookieValue.substring(0, cookieValue.indexOf(PRINCIPAL_TO_TOKEN_SEPARATOR));
        }

        private static String getToken(String cookieValue) {
            return cookieValue.substring(cookieValue.indexOf(PRINCIPAL_TO_TOKEN_SEPARATOR) + 1);
        }

    }
}
