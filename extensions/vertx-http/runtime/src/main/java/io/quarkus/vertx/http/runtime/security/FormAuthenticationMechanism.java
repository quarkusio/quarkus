package io.quarkus.vertx.http.runtime.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.vertx.http.runtime.FormAuthConfig;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class FormAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final Logger log = Logger.getLogger(FormAuthenticationMechanism.class);

    public static final String DEFAULT_POST_LOCATION = "/j_security_check";

    private volatile String loginPage;
    private volatile String errorPage;
    private volatile String postLocation = DEFAULT_POST_LOCATION;
    private volatile String locationCookie = "quarkus-redirect-location";
    private volatile String landingPage = "/index.html";
    private volatile boolean redirectAfterLogin;

    private volatile PersistentLoginManager loginManager;

    private static String encryptionKey;

    public FormAuthenticationMechanism() {
    }

    public void init(HttpConfiguration httpConfiguration, HttpBuildTimeConfig buildTimeConfig) {
        String key;
        if (!httpConfiguration.encryptionKey.isPresent()) {
            if (encryptionKey != null) {
                //persist across dev mode restarts
                key = encryptionKey;
            } else {
                byte[] data = new byte[32];
                new SecureRandom().nextBytes(data);
                key = encryptionKey = Base64.getEncoder().encodeToString(data);
                log.warn("Encryption key was not specified for persistent FORM auth, using temporary key " + key);
            }
        } else {
            key = httpConfiguration.encryptionKey.get();
        }
        FormAuthConfig form = buildTimeConfig.auth.form;
        loginManager = new PersistentLoginManager(key, form.cookieName, form.timeout.toMillis(),
                form.newCookieInterval.toMillis());
        loginPage = form.loginPage.startsWith("/") ? form.loginPage : "/" + form.loginPage;
        errorPage = form.errorPage.startsWith("/") ? form.errorPage : "/" + form.errorPage;
        landingPage = form.landingPage.startsWith("/") ? form.landingPage : "/" + form.landingPage;
        redirectAfterLogin = form.redirectAfterLogin;
    }

    public CompletionStage<SecurityIdentity> runFormAuth(final RoutingContext exchange,
            final IdentityProviderManager securityContext) {
        exchange.request().setExpectMultipart(true);
        CompletableFuture<SecurityIdentity> result = new CompletableFuture<>();
        exchange.request().resume();
        exchange.request().endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                try {
                    MultiMap res = exchange.request().formAttributes();

                    final String jUsername = res.get("j_username");
                    final String jPassword = res.get("j_password");
                    if (jUsername == null || jPassword == null) {
                        log.debugf("Could not authenticate as username or password was not present in the posted result for %s",
                                exchange);
                        result.complete(null);
                        return;
                    }
                    securityContext
                            .authenticate(new UsernamePasswordAuthenticationRequest(jUsername,
                                    new PasswordCredential(jPassword.toCharArray())))
                            .handle(new BiFunction<SecurityIdentity, Throwable, Object>() {
                                @Override
                                public Object apply(SecurityIdentity identity, Throwable throwable) {
                                    if (throwable != null) {
                                        result.completeExceptionally(throwable);
                                    } else {
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
                                        result.complete(null);
                                    }
                                    return null;
                                }
                            });
                } catch (Throwable t) {
                    result.completeExceptionally(t);
                }
            }
        });
        return result;
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

    static CompletionStage<ChallengeData> getRedirect(final RoutingContext exchange, final String location) {
        String loc = exchange.request().scheme() + "://" + exchange.request().host() + location;
        return CompletableFuture.completedFuture(new ChallengeData(302, "Location", loc));
    }

    @Override
    public CompletionStage<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {

        PersistentLoginManager.RestoreResult result = loginManager.restore(context);
        if (result != null) {
            CompletionStage<SecurityIdentity> ret = identityProviderManager
                    .authenticate(new TrustedAuthenticationRequest(result.getPrincipal()));
            ret.thenApply(new Function<SecurityIdentity, Object>() {
                @Override
                public Object apply(SecurityIdentity identity) {
                    loginManager.save(identity, context, result);
                    return null;
                }
            });
            return ret;
        }

        if (context.normalisedPath().endsWith(postLocation) && context.request().method().equals(HttpMethod.POST)) {
            return runFormAuth(context, identityProviderManager);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletionStage<ChallengeData> getChallenge(RoutingContext context) {
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

}
