package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.security.PermissionsAllowed.PERMISSION_TO_ACTION_SEPARATOR;

import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.spi.runtime.MethodDescription;
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

    final RuntimeValue<HttpConfiguration> httpConfiguration;
    final HttpBuildTimeConfig buildTimeConfig;

    //the temp encryption key, persistent across dev mode restarts
    static volatile String encryptionKey;

    public HttpSecurityRecorder(RuntimeValue<HttpConfiguration> httpConfiguration, HttpBuildTimeConfig buildTimeConfig) {
        this.httpConfiguration = httpConfiguration;
        this.buildTimeConfig = buildTimeConfig;
    }

    public Handler<RoutingContext> authenticationMechanismHandler(boolean proactiveAuthentication) {
        return new HttpAuthenticationHandler(proactiveAuthentication);
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

    public BeanContainerListener initPermissions(HttpBuildTimeConfig buildTimeConfig,
            Map<String, Supplier<HttpSecurityPolicy>> policies) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer container) {
                container.beanInstance(PathMatchingHttpSecurityPolicy.class)
                        .init(buildTimeConfig.auth.permissions, policies, buildTimeConfig.rootPath);
            }
        };
    }

    public Supplier<FormAuthenticationMechanism> setupFormAuth() {

        return new Supplier<FormAuthenticationMechanism>() {

            @Override
            public FormAuthenticationMechanism get() {
                String key;
                if (!httpConfiguration.getValue().encryptionKey.isPresent()) {
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
                    key = httpConfiguration.getValue().encryptionKey.get();
                }
                FormAuthConfig form = buildTimeConfig.auth.form;
                PersistentLoginManager loginManager = new PersistentLoginManager(key, form.cookieName, form.timeout.toMillis(),
                        form.newCookieInterval.toMillis(), form.httpOnlyCookie, form.cookieSameSite.name(),
                        form.cookiePath.orElse(null));
                String loginPage = startWithSlash(form.loginPage.orElse(null));
                String errorPage = startWithSlash(form.errorPage.orElse(null));
                String landingPage = startWithSlash(form.landingPage.orElse(null));
                String postLocation = startWithSlash(form.postLocation);
                String usernameParameter = form.usernameParameter;
                String passwordParameter = form.passwordParameter;
                String locationCookie = form.locationCookie;
                String cookiePath = form.cookiePath.orElse(null);
                boolean redirectAfterLogin = form.redirectAfterLogin;
                return new FormAuthenticationMechanism(loginPage, postLocation, usernameParameter, passwordParameter,
                        errorPage, landingPage, redirectAfterLogin, locationCookie, form.cookieSameSite.name(), cookiePath,
                        loginManager);
            }
        };
    }

    private static String startWithSlash(String page) {
        if (page == null) {
            return null;
        }
        return page.startsWith("/") ? page : "/" + page;
    }

    public Supplier<?> setupBasicAuth(HttpBuildTimeConfig buildTimeConfig) {
        return new Supplier<BasicAuthenticationMechanism>() {
            @Override
            public BasicAuthenticationMechanism get() {
                return new BasicAuthenticationMechanism(buildTimeConfig.auth.realm.orElse(null),
                        buildTimeConfig.auth.form.enabled);
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

    public BiFunction<String, String[], Permission> stringPermissionCreator() {
        return StringPermission::new;
    }

    public BiFunction<String, String[], Permission> customPermissionCreator(String clazz, boolean acceptsActions) {
        return new BiFunction<String, String[], Permission>() {
            @Override
            public Permission apply(String name, String[] actions) {
                try {
                    if (acceptsActions) {
                        return (Permission) loadClass(clazz).getConstructors()[0].newInstance(name, actions);
                    } else {
                        return (Permission) loadClass(clazz).getConstructors()[0].newInstance(name);
                    }
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(
                            String.format("Failed to create Permission - class '%s', name '%s', actions '%s'", clazz,
                                    name, Arrays.toString(actions)),
                            e);
                }
            }
        };
    }

    public Supplier<HttpSecurityPolicy> createRolesAllowedPolicy(List<String> rolesAllowed,
            Map<String, List<String>> roleToPermissionsStr, BiFunction<String, String[], Permission> permissionCreator) {
        final Map<String, Set<Permission>> roleToPermissions = createPermissions(roleToPermissionsStr, permissionCreator);
        return new SupplierImpl<>(new RolesAllowedHttpSecurityPolicy(rolesAllowed, roleToPermissions));
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

    private static Map<String, Set<Permission>> createPermissions(Map<String, List<String>> roleToPermissions,
            BiFunction<String, String[], Permission> permissionCreator) {
        // role -> created permissions
        Map<String, Set<Permission>> result = new HashMap<>();
        for (Map.Entry<String, List<String>> e : roleToPermissions.entrySet()) {

            // collect permission actions
            // perm1:action1,perm2:action2,perm1:action3 -> perm1:action1,action3 and perm2:action2
            Map<String, PermissionToActions> cache = new HashMap<>();
            final String role = e.getKey();
            for (String permissionToAction : e.getValue()) {
                // parse permission to actions and add it to cache
                addPermissionToAction(cache, role, permissionToAction);
            }

            // create permissions
            var permissions = new HashSet<Permission>();
            for (PermissionToActions permission : cache.values()) {
                permissions.add(permission.create(permissionCreator));
            }

            result.put(role, Set.copyOf(permissions));
        }
        return Map.copyOf(result);
    }

    private static void addPermissionToAction(Map<String, PermissionToActions> cache, String role, String permissionToAction) {
        final String permissionName;
        final String action;
        // incoming value is either in format perm1:action1 or perm1 (with or withot action)
        if (permissionToAction.contains(PERMISSION_TO_ACTION_SEPARATOR)) {
            // perm1:action1
            var permToActions = permissionToAction.split(PERMISSION_TO_ACTION_SEPARATOR);
            if (permToActions.length != 2) {
                throw new ConfigurationException(
                        String.format("Invalid permission format '%s', please use exactly one permission to action separator",
                                permissionToAction));
            }
            permissionName = permToActions[0].trim();
            action = permToActions[1].trim();
        } else {
            // perm1
            permissionName = permissionToAction.trim();
            action = null;
        }

        if (permissionName.isEmpty()) {
            throw new ConfigurationException(
                    String.format("Invalid permission name '%s' for role '%s'", permissionToAction, role));
        }

        cache.computeIfAbsent(permissionName, new Function<String, PermissionToActions>() {
            @Override
            public PermissionToActions apply(String s) {
                return new PermissionToActions(s);
            }
        }).addAction(action);
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

    static class HttpAuthenticationHandler extends AbstractAuthenticationHandler {

        volatile PathMatchingHttpSecurityPolicy pathMatchingPolicy;

        public HttpAuthenticationHandler(boolean proactiveAuthentication) {
            super(proactiveAuthentication);
        }

        @Override
        protected void setPathMatchingPolicy(RoutingContext event) {
            if (pathMatchingPolicy == null) {
                Instance<PathMatchingHttpSecurityPolicy> pathMatchingPolicyInstance = CDI.current()
                        .select(PathMatchingHttpSecurityPolicy.class);
                pathMatchingPolicy = pathMatchingPolicyInstance.isResolvable() ? pathMatchingPolicyInstance.get() : null;
            }
            if (pathMatchingPolicy != null) {
                event.put(AbstractPathMatchingHttpSecurityPolicy.class.getName(), pathMatchingPolicy);
            }
        }
    }

    public static abstract class AbstractAuthenticationHandler implements Handler<RoutingContext> {
        volatile HttpAuthenticator authenticator;
        final boolean proactiveAuthentication;

        public AbstractAuthenticationHandler(boolean proactiveAuthentication) {
            this.proactiveAuthentication = proactiveAuthentication;
        }

        @Override
        public void handle(RoutingContext event) {
            if (authenticator == null) {
                authenticator = CDI.current().select(HttpAuthenticator.class).get();
            }
            //we put the authenticator into the routing context so it can be used by other systems
            event.put(HttpAuthenticator.class.getName(), authenticator);
            setPathMatchingPolicy(event);

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
                                            .authenticate(AnonymousAuthenticationRequest.INSTANCE);
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

        protected abstract void setPathMatchingPolicy(RoutingContext event);
    }

    private static final class PermissionToActions {
        private final String permissionName;
        private final Set<String> actions;

        private PermissionToActions(String permissionName) {
            this.permissionName = permissionName;
            this.actions = new HashSet<>();
        }

        private void addAction(String action) {
            if (action != null) {
                this.actions.add(action);
            }
        }

        private Permission create(BiFunction<String, String[], Permission> permissionCreator) {
            return permissionCreator.apply(permissionName, actions.toArray(new String[0]));
        }
    }

    private static Class<?> loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load class '" + className + "' for creating permission", e);
        }
    }
}
