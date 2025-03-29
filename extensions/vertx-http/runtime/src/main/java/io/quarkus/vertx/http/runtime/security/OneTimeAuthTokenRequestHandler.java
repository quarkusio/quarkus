package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.fire;
import static io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent.createEmptyLoginEvent;
import static io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent.createOneTimeAuthTokenRequestEvent;
import static io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism.sendRedirect;
import static io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism.startWithSlash;

import java.time.Duration;
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
import io.quarkus.vertx.http.security.token.OneTimeTokenAuthenticator;
import io.quarkus.vertx.http.security.token.OneTimeTokenAuthenticator.RequestInfo;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

final class OneTimeAuthTokenRequestHandler {

    private static final Logger LOG = Logger.getLogger(OneTimeAuthTokenRequestHandler.class);
    private final Event<FormAuthenticationEvent> formAuthEvent;
    private final String onTokenGeneratedRedirectPath;
    private final OneTimeAuthenticationTokenSender tokenSender;
    private final OneTimeTokenAuthenticator tokenAuthenticator;
    private final Duration expiresIn;
    private final String postLocation;

    private OneTimeAuthTokenRequestHandler(Event<FormAuthenticationEvent> formAuthEvent, String onTokenGeneratedRedirectPath,
            OneTimeAuthenticationTokenSender tokenSender, String postLocation, OneTimeTokenAuthenticator tokenAuthenticator,
            Duration expiresIn) {
        this.postLocation = postLocation;
        this.formAuthEvent = formAuthEvent;
        this.onTokenGeneratedRedirectPath = onTokenGeneratedRedirectPath;
        this.tokenSender = tokenSender;
        this.tokenAuthenticator = tokenAuthenticator;
        this.expiresIn = expiresIn;
    }

    void handleTokenRequest(SecurityIdentity identity, RoutingContext routingContext, String userPrincipal) {
        handleTokenRequest(Uni.createFrom().item(identity), routingContext, userPrincipal);
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
                .chain(new Function<Void, Uni<?>>() {
                    @Override
                    public Uni<?> apply(Void unused) {
                        final String redirectLocation;
                        if (event.normalizedPath().endsWith(postLocation)) {
                            redirectLocation = null;
                        } else {
                            redirectLocation = event.request().absoluteURI();
                        }
                        RequestInfo requestInfo = new RequestInfo(redirectLocation, expiresIn);
                        return tokenAuthenticator.store(identity, oneTimeAuthToken, requestInfo);
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

    static OneTimeAuthTokenRequestHandler of(FormAuthRuntimeConfig formConfig, BeanManager beanManager,
            boolean securityEventsEnabled) {
        var formAuthEvent = createFormAuthEvent(beanManager, securityEventsEnabled);
        var oneTimeAuthTokenConfig = formConfig.authenticationToken();
        var arcContainer = Arc.container();
        var tokenSender = arcContainer.select(OneTimeAuthenticationTokenSender.class).get();
        var authenticator = arcContainer.select(OneTimeTokenAuthenticator.class).get();
        return new OneTimeAuthTokenRequestHandler(formAuthEvent,
                startWithSlash(oneTimeAuthTokenConfig.requestRedirectPath().orElse(null)),
                tokenSender, startWithSlash(formConfig.postLocation()), authenticator, oneTimeAuthTokenConfig.expiresIn());
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

}
