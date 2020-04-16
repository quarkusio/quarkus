package io.quarkus.vertx.http.runtime.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.vertx.http.runtime.FormAuthConfig;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class HttpSecurityRecorder {

    private static final Logger log = Logger.getLogger(HttpSecurityRecorder.class);
    protected static final Consumer<Throwable> NOOP_CALLBACK = new Consumer<Throwable>() {
        @Override
        public void accept(Throwable throwable) {

        }
    };

    //the temp encryption key, persistent across dev mode restarts
    static volatile String encryptionKey;

    public Handler<RoutingContext> authenticationMechanismHandler(boolean proactiveAuthentication) {
        return new Handler<RoutingContext>() {

            volatile HttpAuthenticator authenticator;

            @Override
            public void handle(RoutingContext event) {
                if (authenticator == null) {
                    authenticator = CDI.current().select(HttpAuthenticator.class).get();
                }
                //we put the authenticator into the routing context so it can be used by other systems
                event.put(HttpAuthenticator.class.getName(), authenticator);

                //register the default auth failure handler
                //if proactive auth is used this is the only one
                //if using lazy auth this can be modified downstream, to control authentication behaviour
                event.put(QuarkusHttpUser.AUTH_FAILURE_HANDLER, new BiConsumer<RoutingContext, Throwable>() {
                    @Override
                    public void accept(RoutingContext routingContext, Throwable throwable) {
                        while (throwable instanceof CompletionException && throwable.getCause() != null) {
                            throwable = throwable.getCause();
                        }
                        //auth failed
                        if (throwable instanceof AuthenticationFailedException) {
                            authenticator.sendChallenge(event).subscribe().with(new Consumer<Boolean>() {
                                @Override
                                public void accept(Boolean aBoolean) {
                                    event.response().end();
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) {
                                    event.fail(throwable);
                                }
                            });
                        } else if (throwable instanceof AuthenticationCompletionException) {
                            event.response().setStatusCode(401);
                            event.response().end();
                        } else if (throwable instanceof AuthenticationRedirectException) {
                            AuthenticationRedirectException redirectEx = (AuthenticationRedirectException) throwable;
                            event.response().setStatusCode(redirectEx.getCode());
                            event.response().headers().set(HttpHeaders.LOCATION, redirectEx.getRedirectUri());
                            event.response().headers().set(HttpHeaders.CACHE_CONTROL, "no-store");
                            event.response().headers().set("Pragma", "no-cache");
                            event.response().end();
                        } else {
                            event.fail(throwable);
                        }
                    }
                });

                Uni<SecurityIdentity> potentialUser = authenticator.attemptAuthentication(event)
                        .flatMap(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                            @Override
                            public Uni<? extends SecurityIdentity> apply(SecurityIdentity securityIdentity) {
                                //if it is null we use the anonymous identity
                                if (securityIdentity == null) {
                                    return authenticator.getIdentityProviderManager()
                                            .authenticate(AnonymousAuthenticationRequest.INSTANCE);
                                }
                                return Uni.createFrom().item(securityIdentity);
                            }
                        })
                        .onFailure()
                        .invoke(new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) {
                                //handle the auth failure
                                //this can be customised
                                BiConsumer<RoutingContext, Throwable> handler = event.get(QuarkusHttpUser.AUTH_FAILURE_HANDLER);
                                if (handler != null) {
                                    handler.accept(event, throwable);
                                }
                            }
                        })
                        .onItem()
                        .invoke(new Consumer<SecurityIdentity>() {
                            @Override
                            public void accept(SecurityIdentity identity) {
                                //when the result is evaluated we set the user, even if it is evaluated lazily
                                if (identity != null) {
                                    event.setUser(new QuarkusHttpUser(identity));
                                }
                            }
                        });
                if (proactiveAuthentication) {
                    potentialUser
                            .subscribe().with(new Consumer<SecurityIdentity>() {
                                @Override
                                public void accept(SecurityIdentity identity) {
                                    if (event.response().ended()) {
                                        return;
                                    }
                                    event.next();
                                }
                            }, NOOP_CALLBACK);
                } else {
                    event.put(QuarkusHttpUser.DEFERRED_IDENTITY_KEY, potentialUser);
                    event.next();
                }
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
