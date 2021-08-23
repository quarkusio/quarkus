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
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.vertx.http.runtime.FormAuthConfig;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.smallrye.mutiny.tuples.Functions;
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
    public static final String ENCRYPTION_KEY_WAS_NOT_SPECIFIED_FOR_PERSISTENT_FORM_AUTH = "Encryption key was not specified for persistent FORM auth, using temporary key ";

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
                        throwable = extractRootCause(throwable);
                        //auth failed
                        if (throwable instanceof AuthenticationFailedException) {
                            authenticator.sendChallenge(event).subscribe().with(new Consumer<Boolean>() {
                                @Override
                                public void accept(Boolean aBoolean) {
                                    if (!event.response().ended()) {
                                        event.response().end();
                                    }
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

                Uni<SecurityIdentity> potentialUser = authenticator.attemptAuthentication(event).memoize().indefinitely();
                if (proactiveAuthentication) {
                    potentialUser
                            .subscribe().withSubscriber(new UniSubscriber<SecurityIdentity>() {
                                @Override
                                public void onSubscribe(UniSubscription subscription) {

                                }

                                @Override
                                public void onItem(SecurityIdentity identity) {
                                    if (event.response().ended()) {
                                        return;
                                    }
                                    if (identity == null) {
                                        Uni<SecurityIdentity> anon = authenticator.getIdentityProviderManager()
                                                .authenticate(AnonymousAuthenticationRequest.INSTANCE);
                                        anon.subscribe().withSubscriber(new UniSubscriber<SecurityIdentity>() {
                                            @Override
                                            public void onSubscribe(UniSubscription subscription) {

                                            }

                                            @Override
                                            public void onItem(SecurityIdentity item) {
                                                event.put(QuarkusHttpUser.DEFERRED_IDENTITY_KEY, anon);
                                                event.setUser(new QuarkusHttpUser(item));
                                                event.next();
                                            }

                                            @Override
                                            public void onFailure(Throwable failure) {
                                                BiConsumer<RoutingContext, Throwable> handler = event
                                                        .get(QuarkusHttpUser.AUTH_FAILURE_HANDLER);
                                                if (handler != null) {
                                                    handler.accept(event, failure);
                                                }
                                            }
                                        });
                                    } else {//when the result is evaluated we set the user, even if it is evaluated lazily
                                        event.setUser(new QuarkusHttpUser(identity));
                                        event.put(QuarkusHttpUser.DEFERRED_IDENTITY_KEY, potentialUser);
                                        event.next();
                                    }
                                }

                                @Override
                                public void onFailure(Throwable failure) {
                                    //this can be customised
                                    BiConsumer<RoutingContext, Throwable> handler = event
                                            .get(QuarkusHttpUser.AUTH_FAILURE_HANDLER);
                                    if (handler != null) {
                                        handler.accept(event, failure);
                                    }

                                }
                            });
                } else {

                    Uni<SecurityIdentity> lazyUser = potentialUser
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
                            }).onTermination().invoke(new Functions.TriConsumer<SecurityIdentity, Throwable, Boolean>() {
                                @Override
                                public void accept(SecurityIdentity identity, Throwable throwable, Boolean aBoolean) {
                                    if (identity != null) {
                                        //when the result is evaluated we set the user, even if it is evaluated lazily
                                        if (identity != null) {
                                            event.setUser(new QuarkusHttpUser(identity));
                                        }
                                    } else if (throwable != null) {
                                        //handle the auth failure
                                        //this can be customised
                                        BiConsumer<RoutingContext, Throwable> handler = event
                                                .get(QuarkusHttpUser.AUTH_FAILURE_HANDLER);
                                        if (handler != null) {
                                            handler.accept(event, throwable);
                                        }
                                    }
                                }
                            }).memoize().indefinitely();
                    event.put(QuarkusHttpUser.DEFERRED_IDENTITY_KEY, lazyUser);
                    event.next();
                }
            }
        };
    }

    private Throwable extractRootCause(Throwable throwable) {
        while ((throwable instanceof CompletionException && throwable.getCause() != null) ||
                (throwable instanceof CompositeException)) {
            if (throwable instanceof CompositeException) {
                throwable = ((CompositeException) throwable).getCauses().get(0);
            } else {
                throwable = throwable.getCause();
            }
        }
        return throwable;
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
                        log.warn(ENCRYPTION_KEY_WAS_NOT_SPECIFIED_FOR_PERSISTENT_FORM_AUTH + key);
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
                String postLocation = form.postLocation.startsWith("/") ? form.postLocation : "/" + form.postLocation;
                String usernameParameter = form.usernameParameter;
                String passwordParameter = form.passwordParameter;
                String locationCookie = form.locationCookie;
                boolean redirectAfterLogin = form.redirectAfterLogin;
                return new FormAuthenticationMechanism(loginPage, postLocation, usernameParameter, passwordParameter,
                        errorPage, landingPage, redirectAfterLogin, locationCookie, loginManager);
            }
        };
    }

    public Supplier<?> setupBasicAuth(HttpBuildTimeConfig buildTimeConfig) {
        return new Supplier<BasicAuthenticationMechanism>() {
            @Override
            public BasicAuthenticationMechanism get() {
                return new BasicAuthenticationMechanism(buildTimeConfig.auth.realm, buildTimeConfig.auth.form.enabled);
            }
        };
    }

    public Supplier<?> setupMtlsClientAuth() {
        return new Supplier<MtlsAuthenticationMechanism>() {
            @Override
            public MtlsAuthenticationMechanism get() {
                return new MtlsAuthenticationMechanism();
            }
        };
    }
}
