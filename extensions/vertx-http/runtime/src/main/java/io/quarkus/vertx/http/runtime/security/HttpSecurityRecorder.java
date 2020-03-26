package io.quarkus.vertx.http.runtime.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.FormAuthConfig;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class HttpSecurityRecorder {

    private static final Logger log = Logger.getLogger(HttpSecurityRecorder.class);

    //the temp encryption key, persistent across dev mode restarts
    static volatile String encryptionKey;

    public Handler<RoutingContext> authenticationMechanismHandler() {
        return new Handler<RoutingContext>() {

            volatile HttpAuthenticator authenticator;

            @Override
            public void handle(RoutingContext event) {
                if (authenticator == null) {
                    authenticator = CDI.current().select(HttpAuthenticator.class).get();
                }
                //we put the authenticator into the routing context so it can be used by other systems
                event.put(HttpAuthenticator.class.getName(), authenticator);
                authenticator.attemptAuthentication(event).handle(new BiFunction<SecurityIdentity, Throwable, Object>() {
                    @Override
                    public Object apply(SecurityIdentity identity, Throwable throwable) {
                        if (throwable != null) {
                            while (throwable instanceof CompletionException && throwable.getCause() != null) {
                                throwable = throwable.getCause();
                            }
                            //auth failed
                            if (throwable instanceof AuthenticationFailedException) {
                                authenticator.sendChallenge(event, new Runnable() {
                                    @Override
                                    public void run() {
                                        event.response().end();
                                    }
                                }).exceptionally(new Function<Throwable, Void>() {
                                    @Override
                                    public Void apply(Throwable throwable) {
                                        event.fail(throwable);
                                        return null;
                                    }
                                });
                            } else if (throwable instanceof AuthenticationCompletionException) {
                                event.response().setStatusCode(401);
                                event.response().end();
                            } else if (throwable instanceof AuthenticationRedirectException) {
                                AuthenticationRedirectException redirectEx = (AuthenticationRedirectException) throwable;
                                event.response().setStatusCode(redirectEx.getCode());
                                event.response().headers().set(HttpHeaders.LOCATION, redirectEx.getRedirectUri());
                                event.response().end();
                            } else {
                                event.fail(throwable);
                            }
                            return null;
                        }
                        if (event.response().ended()) {
                            return null;
                        }
                        if (identity != null) {
                            event.setUser(new QuarkusHttpUser(identity));
                        }
                        event.next();
                        return null;
                    }
                });
            }
        };
    }

    public Handler<RoutingContext> permissionCheckHandler() {
        return new Handler<RoutingContext>() {
            volatile HttpAuthorizer authorizer;

            @Override
            public void handle(RoutingContext event) {
                if (authorizer == null) {
                    authorizer = CDI.current().select(HttpAuthorizer.class).get();
                }
                authorizer.checkPermission(event);
            }
        };
    }

    public BeanContainerListener initPermissions(HttpBuildTimeConfig permissions,
            Map<String, Supplier<HttpSecurityPolicy>> policies) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer container) {
                container.instance(PathMatchingHttpSecurityPolicy.class).init(permissions, policies);
            }
        };
    }

    public Supplier<FormAuthenticationMechanism> setupFormAuth(HttpConfiguration httpConfiguration,
            HttpBuildTimeConfig buildTimeConfig) {

        return new Supplier<FormAuthenticationMechanism>() {
            @Override
            public FormAuthenticationMechanism get() {
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
                PersistentLoginManager loginManager = new PersistentLoginManager(key, form.cookieName, form.timeout.toMillis(),
                        form.newCookieInterval.toMillis());
                String loginPage = form.loginPage.startsWith("/") ? form.loginPage : "/" + form.loginPage;
                String errorPage = form.errorPage.startsWith("/") ? form.errorPage : "/" + form.errorPage;
                String landingPage = form.landingPage.startsWith("/") ? form.landingPage : "/" + form.landingPage;
                boolean redirectAfterLogin = form.redirectAfterLogin;
                return new FormAuthenticationMechanism(loginPage, errorPage, landingPage, redirectAfterLogin, loginManager);
            }
        };
    }

    public Supplier<?> setupBasicAuth(HttpBuildTimeConfig buildTimeConfig) {
        return new Supplier<BasicAuthenticationMechanism>() {
            @Override
            public BasicAuthenticationMechanism get() {
                return new BasicAuthenticationMechanism(buildTimeConfig.auth.realm, "BASIC", buildTimeConfig.auth.form.enabled);
            }
        };
    }
}
