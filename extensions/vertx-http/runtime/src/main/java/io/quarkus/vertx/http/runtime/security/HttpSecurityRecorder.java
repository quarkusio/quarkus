package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.setRoutingContextAttribute;
import static io.quarkus.vertx.http.runtime.security.RolesMapping.ROLES_MAPPING_KEY;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.security.token.OneTimeAuthenticationTokenSender;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.smallrye.mutiny.tuples.Functions;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class HttpSecurityRecorder {

    private static final Logger log = Logger.getLogger(HttpSecurityRecorder.class);

    public RuntimeValue<AuthenticationHandler> authenticationMechanismHandler(boolean proactiveAuthentication,
            boolean propagateRoutingContext) {
        return new RuntimeValue<>(new AuthenticationHandler(proactiveAuthentication, propagateRoutingContext));
    }

    public Handler<RoutingContext> getHttpAuthenticatorHandler(RuntimeValue<AuthenticationHandler> handlerRuntimeValue) {
        return handlerRuntimeValue.getValue();
    }

    public void initializeHttpAuthenticatorHandler(RuntimeValue<AuthenticationHandler> handlerRuntimeValue,
            VertxHttpConfig httpConfig, BeanContainer beanContainer) {
        handlerRuntimeValue.getValue().init(beanContainer.beanInstance(PathMatchingHttpSecurityPolicy.class),
                RolesMapping.of(httpConfig.auth().rolesMapping()));
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

    /**
     * This handler resolves the identity, and will be mapped to the post location. Otherwise,
     * for lazy auth the post will not be evaluated if there is no security rule for the post location.
     */
    public void formAuthPostHandler(RuntimeValue<Router> httpRouter, VertxHttpConfig httpConfiguration) {
        httpRouter.getValue().post(httpConfiguration.auth().form().postLocation())
                .handler(new Handler<RoutingContext>() {
                    @Override
                    public void handle(RoutingContext event) {
                        Uni<SecurityIdentity> user = event.get(QuarkusHttpUser.DEFERRED_IDENTITY_KEY);
                        user.subscribe().withSubscriber(new UniSubscriber<SecurityIdentity>() {
                            @Override
                            public void onSubscribe(UniSubscription uniSubscription) {

                            }

                            @Override
                            public void onItem(SecurityIdentity securityIdentity) {
                                // we expect that form-based authentication mechanism to recognize the post-location,
                                // authenticate and if user provided credentials in form attribute, response will be ended
                                if (!event.response().ended()) {
                                    event.response().end();
                                }
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                // with current builtin implementation if only form-based authentication mechanism the event here
                                // won't be ended or failed, but we check in case there is custom implementation that differs
                                if (!event.response().ended() && !event.failed()) {
                                    event.fail(throwable);
                                }
                            }
                        });
                    }
                });
    }

    public Supplier<EagerSecurityInterceptorStorage> createSecurityInterceptorStorage(
            Map<RuntimeValue<MethodDescription>, Consumer<RoutingContext>> endpointRuntimeValToInterceptor,
            Map<String, Consumer<RoutingContext>> classNameToInterceptor) {

        final Map<MethodDescription, Consumer<RoutingContext>> endpointToInterceptor = new HashMap<>();
        for (var entry : endpointRuntimeValToInterceptor.entrySet()) {
            endpointToInterceptor.put(entry.getKey().getValue(), entry.getValue());
        }

        return new Supplier<EagerSecurityInterceptorStorage>() {
            @Override
            public EagerSecurityInterceptorStorage get() {
                return new EagerSecurityInterceptorStorage(endpointToInterceptor, classNameToInterceptor);
            }
        };
    }

    public Supplier<Map<String, Object>> createAdditionalSecEventPropsSupplier() {
        return new Supplier<Map<String, Object>>() {
            @Override
            public Map<String, Object> get() {
                if (Arc.container().requestContext().isActive()) {

                    // if present, add RoutingContext from CDI request to the SecurityEvents produced in Security extension
                    // it's done this way as Security extension is not Vert.x based, but users find RoutingContext useful
                    var event = Arc.container().instance(CurrentVertxRequest.class).get().getCurrent();
                    if (event != null) {

                        if (event.user() instanceof QuarkusHttpUser user) {
                            return Map.of(RoutingContext.class.getName(), event, SecurityIdentity.class.getName(),
                                    user.getSecurityIdentity());
                        }

                        return Map.of(RoutingContext.class.getName(), event);
                    }
                }
                return Map.of();
            }
        };
    }

    public void validateOneTimeAuthToken(boolean tokenSenderNotFound, VertxHttpConfig httpConfig) {
        if (tokenSenderNotFound && httpConfig.auth().form().authenticationToken().enabled()) {
            throw new ConfigurationException(
                    "One-time authentication token feature is enabled, but no '%s' interface has been found"
                            .formatted(OneTimeAuthenticationTokenSender.class.getName()),
                    Set.of("quarkus.http.auth.form.authentication-token.enabled"));
        }
    }

    public static abstract class DefaultAuthFailureHandler implements BiConsumer<RoutingContext, Throwable> {

        /**
         * A {@link RoutingContext#get(String)} key added for exceptions raised during authentication that are not
         * the {@link io.quarkus.security.AuthenticationException}.
         */
        private static final String OTHER_AUTHENTICATION_FAILURE = "io.quarkus.vertx.http.runtime.security.other-auth-failure";
        static final String DEV_MODE_AUTHENTICATION_FAILURE_BODY = "io.quarkus.vertx.http.runtime.security.dev-mode.auth-failure-body";

        protected DefaultAuthFailureHandler() {
        }

        @Override
        public void accept(RoutingContext event, Throwable throwable) {
            if (event.response().ended()) {
                return;
            }
            throwable = extractRootCause(throwable);
            if (LaunchMode.isDev() && throwable instanceof AuthenticationException
                    && throwable.getMessage() != null) {
                event.put(DEV_MODE_AUTHENTICATION_FAILURE_BODY, throwable.getMessage());
            }
            //auth failed
            if (throwable instanceof AuthenticationFailedException authenticationFailedException) {
                getAuthenticator(event).sendChallenge(event).subscribe().with(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {
                        if (!event.response().ended()) {
                            proceed(authenticationFailedException);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        event.fail(throwable);
                    }
                });
            } else if (throwable instanceof AuthenticationCompletionException) {
                log.debug("Authentication has failed, returning HTTP status 401");
                event.response().setStatusCode(401);
                proceed(throwable);
            } else if (throwable instanceof AuthenticationRedirectException redirectEx) {
                event.response().setStatusCode(redirectEx.getCode());
                event.response().headers().set(HttpHeaders.LOCATION, redirectEx.getRedirectUri());
                event.response().headers().set(HttpHeaders.CACHE_CONTROL, "no-store");
                event.response().headers().set("Pragma", "no-cache");
                proceed(throwable);
            } else {
                event.put(OTHER_AUTHENTICATION_FAILURE, Boolean.TRUE);
                event.fail(throwable);
            }
        }

        protected abstract void proceed(Throwable throwable);

        private static HttpAuthenticator getAuthenticator(RoutingContext event) {
            return event.get(HttpAuthenticator.class.getName());
        }

        public static Throwable extractRootCause(Throwable throwable) {
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

        public static void markIfOtherAuthenticationFailure(RoutingContext event, Throwable throwable) {
            if (!(throwable instanceof AuthenticationException)) {
                event.put(OTHER_AUTHENTICATION_FAILURE, Boolean.TRUE);
            }
        }

        public static void removeMarkAsOtherAuthenticationFailure(RoutingContext event) {
            event.remove(OTHER_AUTHENTICATION_FAILURE);
        }

        public static boolean isOtherAuthenticationFailure(RoutingContext event) {
            return Boolean.TRUE.equals(event.get(OTHER_AUTHENTICATION_FAILURE));
        }
    }

    public static final class AuthenticationHandler implements Handler<RoutingContext> {
        volatile HttpAuthenticator authenticator;
        private final boolean proactiveAuthentication;
        private final boolean propagateRoutingContext;
        private AbstractPathMatchingHttpSecurityPolicy pathMatchingPolicy;
        private RolesMapping rolesMapping;

        AuthenticationHandler(boolean proactiveAuthentication, boolean propagateRoutingContext) {
            this.proactiveAuthentication = proactiveAuthentication;
            this.propagateRoutingContext = propagateRoutingContext;
        }

        public AuthenticationHandler(boolean proactiveAuthentication) {
            this(proactiveAuthentication, false);
        }

        @Override
        public void handle(RoutingContext event) {
            if (authenticator == null) {
                // this needs to be lazily initialized as the way some identity providers are created requires that
                // all the build items are finished before this is called (for example Elytron identity providers use
                // SecurityDomain that is not ready when identity providers are ready; it's racy)
                authenticator = CDI.current().select(HttpAuthenticator.class).get();
            }
            if (propagateRoutingContext) {
                Context context = Vertx.currentContext();
                if (context != null && VertxContext.isDuplicatedContext(context)) {
                    context.putLocal(HttpSecurityUtils.ROUTING_CONTEXT_ATTRIBUTE, event);
                }
            }
            //we put the authenticator into the routing context so it can be used by other systems
            event.put(HttpAuthenticator.class.getName(), authenticator);
            if (pathMatchingPolicy != null) {
                event.put(AbstractPathMatchingHttpSecurityPolicy.class.getName(), pathMatchingPolicy);
            }
            if (rolesMapping != null) {
                event.put(ROLES_MAPPING_KEY, rolesMapping);
            }

            //register the default auth failure handler
            if (proactiveAuthentication) {
                //if proactive auth is used this is the only one
                event.put(QuarkusHttpUser.AUTH_FAILURE_HANDLER, new DefaultAuthFailureHandler() {
                    @Override
                    protected void proceed(Throwable throwable) {

                        if (!event.failed()) {
                            //failing event makes it possible to customize response via failure handlers
                            //QuarkusErrorHandler will send response if no other failure handler did
                            event.fail(throwable);
                        }
                    }
                });
            } else {
                //if using lazy auth this can be modified downstream, to control authentication behaviour
                event.put(QuarkusHttpUser.AUTH_FAILURE_HANDLER, new DefaultAuthFailureHandler() {
                    @Override
                    protected void proceed(Throwable throwable) {
                        //we can't fail event here as request processing has already begun (e.g. in RESTEasy Reactive)
                        //and extensions may have their ways to handle failures
                        if (throwable instanceof AuthenticationCompletionException
                                && throwable.getMessage() != null
                                && LaunchMode.current() == LaunchMode.DEVELOPMENT) {
                            event.end(throwable.getMessage());
                        } else {
                            event.end();
                        }
                    }
                });
            }

            if (proactiveAuthentication) {
                Uni<SecurityIdentity> potentialUser = authenticator.attemptAuthentication(event).memoize().indefinitely();
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
                                            .authenticate(
                                                    setRoutingContextAttribute(new AnonymousAuthenticationRequest(), event));
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

                Uni<SecurityIdentity> lazyUser = Uni
                        .createFrom()
                        .nullItem()
                        // Only attempt to authenticate if required
                        .flatMap(n -> authenticator.attemptAuthentication(event))
                        .memoize()
                        .indefinitely()
                        .flatMap(new Function<SecurityIdentity, Uni<? extends SecurityIdentity>>() {
                            @Override
                            public Uni<? extends SecurityIdentity> apply(SecurityIdentity securityIdentity) {
                                //if it is null we use the anonymous identity
                                if (securityIdentity == null) {
                                    return authenticator.getIdentityProviderManager()
                                            .authenticate(
                                                    setRoutingContextAttribute(new AnonymousAuthenticationRequest(), event));
                                }
                                return Uni.createFrom().item(securityIdentity);
                            }
                        }).onTermination().invoke(new Functions.TriConsumer<SecurityIdentity, Throwable, Boolean>() {
                            @Override
                            public void accept(SecurityIdentity identity, Throwable throwable, Boolean aBoolean) {
                                if (identity != null) {
                                    //when the result is evaluated we set the user, even if it is evaluated lazily
                                    event.setUser(new QuarkusHttpUser(identity));
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

        // this must happen before the router is finalized, so that class members are set before any concurrency happens
        public void init(AbstractPathMatchingHttpSecurityPolicy pathMatchingPolicy,
                RolesMapping rolesMapping) {
            // null checks in this method are here because this is a public method
            // but class members should be initialized once, before the router is finalized
            if (this.pathMatchingPolicy == null) {
                this.pathMatchingPolicy = pathMatchingPolicy;
            }
            if (this.rolesMapping == null) {
                this.rolesMapping = rolesMapping;
            }
        }
    }

    public void setMtlsCertificateRoleProperties(VertxHttpConfig httpConfig) {
        InstanceHandle<MtlsAuthenticationMechanism> mtls = Arc.container().instance(MtlsAuthenticationMechanism.class);

        if (mtls.isAvailable() && httpConfig.auth().certificateRoleProperties().isPresent()) {
            Path rolesPath = httpConfig.auth().certificateRoleProperties().get();
            URL rolesResource = null;
            if (Files.exists(rolesPath)) {
                try {
                    rolesResource = rolesPath.toUri().toURL();
                } catch (MalformedURLException e) {
                    // The Files.exists(rolesPath) check has succeeded therefore this exception can't happen in this case
                }
            } else {
                rolesResource = Thread.currentThread().getContextClassLoader().getResource(rolesPath.toString());
            }
            if (rolesResource == null) {
                throw new ConfigurationException(
                        "quarkus.http.auth.certificate-role-properties location can not be resolved",
                        Set.of("quarkus.http.auth.certificate-role-properties"));
            }

            try (Reader reader = new BufferedReader(
                    new InputStreamReader(rolesResource.openStream(), StandardCharsets.UTF_8))) {
                Properties rolesProps = new Properties();
                rolesProps.load(reader);

                Map<String, Set<String>> roles = new HashMap<>();
                for (Map.Entry<Object, Object> e : rolesProps.entrySet()) {
                    log.debugf("Added role mapping for %s:%s", e.getKey(), e.getValue());
                    roles.put((String) e.getKey(), parseRoles((String) e.getValue()));
                }

                if (!roles.isEmpty()) {
                    var certRolesAttribute = new CertificateRoleAttribute(httpConfig.auth().certificateRoleAttribute(), roles);
                    mtls.get().setCertificateToRolesMapper(certRolesAttribute.rolesMapper());
                }
            } catch (Exception e) {
                log.warnf("Unable to read roles mappings from %s:%s", rolesPath, e.getMessage());
            }
        }
    }

    public RuntimeValue<MethodDescription> createMethodDescription(String className, String methodName, String[] paramTypes) {
        return new RuntimeValue<>(new MethodDescription(className, methodName, paramTypes));
    }

    public Function<String, Consumer<RoutingContext>> authMechanismSelectionInterceptorCreator() {
        return new Function<String, Consumer<RoutingContext>>() {
            @Override
            public Consumer<RoutingContext> apply(String authMechanismName) {
                // when endpoint is annotated with @HttpAuthenticationMechanism("my-mechanism"), we add this mechanism
                // to the event so that when request is being authenticated, the HTTP authenticator will know
                // what mechanism should be used
                return new Consumer<RoutingContext>() {
                    @Override
                    public void accept(RoutingContext routingContext) {
                        HttpAuthenticator.selectAuthMechanism(routingContext, authMechanismName);
                    }
                };
            }
        };
    }

    public RuntimeValue<List<String>> getSecurityIdentityContextKeySupplier() {
        return new RuntimeValue<>(List.of(HttpSecurityUtils.ROUTING_CONTEXT_ATTRIBUTE));
    }

    public Consumer<RoutingContext> createEagerSecurityInterceptor(
            Function<String, Consumer<RoutingContext>> interceptorCreator, String annotationValue) {
        return interceptorCreator.apply(annotationValue);
    }

    public Consumer<RoutingContext> compoundSecurityInterceptor(Consumer<RoutingContext> interceptor1,
            Consumer<RoutingContext> interceptor2) {
        return new Consumer<RoutingContext>() {
            @Override
            public void accept(RoutingContext routingContext) {
                interceptor1.accept(routingContext);
                interceptor2.accept(routingContext);
            }
        };
    }

    public void selectAuthMechanismViaAnnotation() {
        HttpAuthenticator.selectAuthMechanismWithAnnotation();
    }

    private static Set<String> parseRoles(String value) {
        Set<String> roles = new HashSet<>();
        for (String s : value.split(",")) {
            roles.add(s.trim());
        }
        return Set.copyOf(roles);
    }

    public Supplier<BasicAuthenticationMechanism> basicAuthenticationMechanismBean(VertxHttpConfig httpConfig,
            boolean formAuthEnabled) {
        return new Supplier<>() {
            @Override
            public BasicAuthenticationMechanism get() {
                return new BasicAuthenticationMechanism(httpConfig.auth().realm().orElse(null), formAuthEnabled);
            }
        };
    }

}
