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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
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

    public RuntimeValue<AuthenticationHandler> authenticationMechanismHandler(boolean proactiveAuthentication) {
        return new RuntimeValue<>(new AuthenticationHandler(proactiveAuthentication));
    }

    public Handler<RoutingContext> getHttpAuthenticatorHandler(RuntimeValue<AuthenticationHandler> handlerRuntimeValue) {
        return handlerRuntimeValue.getValue();
    }

    public void initializeHttpAuthenticatorHandler(RuntimeValue<AuthenticationHandler> handlerRuntimeValue,
            HttpConfiguration httpConfig) {
        handlerRuntimeValue.getValue().init(PathMatchingHttpSecurityPolicy.class,
                RolesMapping.of(httpConfig.auth.rolesMapping));
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
    public Handler<RoutingContext> formAuthPostHandler() {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                Uni<SecurityIdentity> user = event.get(QuarkusHttpUser.DEFERRED_IDENTITY_KEY);
                user.subscribe().withSubscriber(new UniSubscriber<SecurityIdentity>() {
                    @Override
                    public void onSubscribe(UniSubscription uniSubscription) {

                    }

                    @Override
                    public void onItem(SecurityIdentity securityIdentity) {
                        event.next();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        event.fail(throwable);
                    }
                });
            }
        };
    }

    public Supplier<EagerSecurityInterceptorStorage> createSecurityInterceptorStorage(
            Map<RuntimeValue<MethodDescription>, Consumer<RoutingContext>> endpointRuntimeValToInterceptor) {

        final Map<MethodDescription, Consumer<RoutingContext>> endpointToInterceptor = new HashMap<>();
        for (var entry : endpointRuntimeValToInterceptor.entrySet()) {
            endpointToInterceptor.put(entry.getKey().getValue(), entry.getValue());
        }

        return new Supplier<EagerSecurityInterceptorStorage>() {
            @Override
            public EagerSecurityInterceptorStorage get() {
                return new EagerSecurityInterceptorStorage(endpointToInterceptor);
            }
        };
    }

    public RuntimeValue<HttpSecurityPolicy> createNamedHttpSecurityPolicy(Supplier<HttpSecurityPolicy> policySupplier,
            String name) {
        return new RuntimeValue<>(new HttpSecurityPolicy() {
            private final HttpSecurityPolicy delegate = policySupplier.get();

            @Override
            public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
                    AuthorizationRequestContext requestContext) {
                return delegate.checkPermission(request, identity, requestContext);
            }

            @Override
            public String name() {
                return name;
            }
        });
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

    public static abstract class DefaultAuthFailureHandler implements BiConsumer<RoutingContext, Throwable> {

        protected DefaultAuthFailureHandler() {
        }

        @Override
        public void accept(RoutingContext event, Throwable throwable) {
            if (event.response().ended()) {
                return;
            }
            throwable = extractRootCause(throwable);
            //auth failed
            if (throwable instanceof AuthenticationFailedException) {
                AuthenticationFailedException authenticationFailedException = (AuthenticationFailedException) throwable;
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
            } else if (throwable instanceof AuthenticationRedirectException) {
                AuthenticationRedirectException redirectEx = (AuthenticationRedirectException) throwable;
                event.response().setStatusCode(redirectEx.getCode());
                event.response().headers().set(HttpHeaders.LOCATION, redirectEx.getRedirectUri());
                event.response().headers().set(HttpHeaders.CACHE_CONTROL, "no-store");
                event.response().headers().set("Pragma", "no-cache");
                proceed(throwable);
            } else {
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
    }

    public static final class AuthenticationHandler implements Handler<RoutingContext> {
        volatile HttpAuthenticator authenticator;
        private final boolean proactiveAuthentication;
        private AbstractPathMatchingHttpSecurityPolicy pathMatchingPolicy;
        private RolesMapping rolesMapping;

        public AuthenticationHandler(boolean proactiveAuthentication) {
            this.proactiveAuthentication = proactiveAuthentication;
        }

        @Override
        public void handle(RoutingContext event) {
            if (authenticator == null) {
                authenticator = CDI.current().select(HttpAuthenticator.class).get();
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
                        event.end();
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

        public void init(Class<? extends AbstractPathMatchingHttpSecurityPolicy> pathMatchingPolicyClass,
                RolesMapping rolesMapping) {
            if (pathMatchingPolicy == null) {
                var pathMatchingPolicyInstance = CDI.current().select(pathMatchingPolicyClass);
                if (pathMatchingPolicyInstance.isResolvable() && !pathMatchingPolicyInstance.get().hasNoPermissions()) {
                    pathMatchingPolicy = pathMatchingPolicyInstance.get();
                } else {
                    pathMatchingPolicy = null;
                }
            }
            if (this.rolesMapping == null) {
                this.rolesMapping = rolesMapping;
            }
        }
    }

    public void setMtlsCertificateRoleProperties(HttpConfiguration config) {
        InstanceHandle<MtlsAuthenticationMechanism> mtls = Arc.container().instance(MtlsAuthenticationMechanism.class);

        if (mtls.isAvailable() && config.auth.certificateRoleProperties.isPresent()) {
            Path rolesPath = config.auth.certificateRoleProperties.get();
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
                    var certRolesAttribute = new CertificateRoleAttribute(config.auth.certificateRoleAttribute, roles);
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

}
