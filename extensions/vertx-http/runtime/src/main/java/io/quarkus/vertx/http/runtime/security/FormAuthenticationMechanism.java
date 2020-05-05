package io.quarkus.vertx.http.runtime.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

public class FormAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final Logger log = Logger.getLogger(FormAuthenticationMechanism.class);

    public static final String DEFAULT_POST_LOCATION = "/j_security_check";

    private final String loginPage;
    private final String errorPage;
    private final String postLocation = DEFAULT_POST_LOCATION;
    private final String locationCookie = "quarkus-redirect-location";
    private final String landingPage;
    private final boolean redirectAfterLogin;

    private final PersistentLoginManager loginManager;

    public FormAuthenticationMechanism(String loginPage, String errorPage, String landingPage, boolean redirectAfterLogin,
            PersistentLoginManager loginManager) {
        this.loginPage = loginPage;
        this.errorPage = errorPage;
        this.landingPage = landingPage;
        this.redirectAfterLogin = redirectAfterLogin;
        this.loginManager = loginManager;
    }

    public Uni<SecurityIdentity> runFormAuth(final RoutingContext exchange,
            final IdentityProviderManager securityContext) {
        exchange.request().setExpectMultipart(true);
        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super SecurityIdentity>>() {
            @Override
            public void accept(UniEmitter<? super SecurityIdentity> uniEmitter) {
                exchange.request().endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        try {
                            MultiMap res = exchange.request().formAttributes();

                            final String jUsername = res.get("j_username");
                            final String jPassword = res.get("j_password");
                            if (jUsername == null || jPassword == null) {
                                log.debugf(
                                        "Could not authenticate as username or password was not present in the posted result for %s",
                                        exchange);
                                uniEmitter.complete(null);
                                return;
                            }
                            securityContext
                                    .authenticate(new UsernamePasswordAuthenticationRequest(jUsername,
                                            new PasswordCredential(jPassword.toCharArray())))
                                    .subscribe().with(new Consumer<SecurityIdentity>() {
                                        @Override
                                        public void accept(SecurityIdentity identity) {
                                            loginManager.save(identity, exchange, null);
                                            if (redirectAfterLogin || exchange.getCookie(locationCookie) != null) {
                                                handleRedirectBack(exchange);
                                                //we  have authenticated, but we want to just redirect back to the original page
                                                //so we don't actually authenticate the current request
                                                //instead we have just set a cookie so the redirected request will be authenticated
                                            } else {
                                                exchange.response().setStatusCode(200);
                                                exchange.response().end();
                                            }
                                            uniEmitter.complete(null);
                                        }
                                    }, new Consumer<Throwable>() {
                                        @Override
                                        public void accept(Throwable throwable) {
                                            uniEmitter.fail(throwable);
                                        }
                                    });
                        } catch (Throwable t) {
                            uniEmitter.fail(t);
                        }
                    }
                });
                exchange.request().resume();
            }
        });
    }

    protected void handleRedirectBack(final RoutingContext exchange) {
        Cookie redirect = exchange.getCookie(locationCookie);
        String location;
        if (redirect != null) {
            location = redirect.getValue();
            exchange.response().addCookie(redirect.setMaxAge(0));
        } else {
            location = exchange.request().scheme() + "://" + exchange.request().host() + landingPage;
        }
        exchange.response().setStatusCode(302);
        exchange.response().headers().add(HttpHeaderNames.LOCATION, location);
        exchange.response().end();
    }

    protected void storeInitialLocation(final RoutingContext exchange) {
        exchange.response().addCookie(Cookie.cookie(locationCookie, exchange.request().absoluteURI()).setPath("/"));
    }

    protected void servePage(final RoutingContext exchange, final String location) {
        sendRedirect(exchange, location);
    }

    static void sendRedirect(final RoutingContext exchange, final String location) {
        String loc = exchange.request().scheme() + "://" + exchange.request().host() + location;
        exchange.response().headers().add(HttpHeaderNames.LOCATION, loc);
        exchange.response().setStatusCode(302);
        exchange.response().end();
    }

    static Uni<ChallengeData> getRedirect(final RoutingContext exchange, final String location) {
        String loc = exchange.request().scheme() + "://" + exchange.request().host() + location;
        return Uni.createFrom().item(new ChallengeData(302, "Location", loc));
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {

        PersistentLoginManager.RestoreResult result = loginManager.restore(context);
        if (result != null) {
            Uni<SecurityIdentity> ret = identityProviderManager
                    .authenticate(new TrustedAuthenticationRequest(result.getPrincipal()));
            return ret.onItem().invoke(new Consumer<SecurityIdentity>() {
                @Override
                public void accept(SecurityIdentity securityIdentity) {
                    loginManager.save(securityIdentity, context, result);
                }
            });
        }

        if (context.normalisedPath().endsWith(postLocation) && context.request().method().equals(HttpMethod.POST)) {
            return runFormAuth(context, identityProviderManager);
        } else {
            return Uni.createFrom().optional(Optional.empty());
        }
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        if (context.normalisedPath().endsWith(postLocation) && context.request().method().equals(HttpMethod.POST)) {
            log.debugf("Serving form auth error page %s for %s", loginPage, context);
            // This method would no longer be called if authentication had already occurred.
            return getRedirect(context, errorPage);
        } else {
            log.debugf("Serving login form %s for %s", loginPage, context);
            // we need to store the URL
            storeInitialLocation(context);
            return getRedirect(context, loginPage);
        }
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return new HashSet<>(Arrays.asList(UsernamePasswordAuthenticationRequest.class, TrustedAuthenticationRequest.class));
    }

    @Override
    public HttpCredentialTransport getCredentialTransport() {
        return new HttpCredentialTransport(HttpCredentialTransport.Type.POST, postLocation);
    }
}
