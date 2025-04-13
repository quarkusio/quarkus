package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcAuthenticationPolicy.AUTHENTICATION_POLICY_KEY;
import static io.quarkus.oidc.runtime.OidcConfig.getDefaultTenant;
import static io.quarkus.oidc.runtime.OidcUtils.DEFAULT_TENANT_ID;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getRoutingContextAttribute;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.util.TypeLiteral;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.Oidc;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantIdentityProvider;
import io.quarkus.oidc.runtime.OidcAuthenticationPolicy.OAuth2StepUpAuthenticationPolicy;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RuntimeInit;
import io.quarkus.runtime.annotations.StaticInit;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.SecurityConfig;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class OidcRecorder {

    static final Logger LOG = Logger.getLogger(OidcRecorder.class);

    public Supplier<DefaultTokenIntrospectionUserInfoCache> setupTokenCache(OidcConfig config, Supplier<Vertx> vertx) {
        return new Supplier<DefaultTokenIntrospectionUserInfoCache>() {
            @Override
            public DefaultTokenIntrospectionUserInfoCache get() {
                return new DefaultTokenIntrospectionUserInfoCache(config, vertx.get());
            }
        };
    }

    @StaticInit
    public void setUserInfoInjectionPointDetected(boolean userInfoInjectionPointDetected) {
        TenantContextFactory.userInfoInjectionPointDetected = userInfoInjectionPointDetected;
    }

    @RuntimeInit
    public Function<SyntheticCreationalContext<TenantConfigBean>, TenantConfigBean> createTenantConfigBean(
            OidcConfig config, Supplier<Vertx> vertx, Supplier<TlsConfigurationRegistry> registry,
            SecurityConfig securityConfig) {
        return new Function<SyntheticCreationalContext<TenantConfigBean>, TenantConfigBean>() {
            @Override
            public TenantConfigBean apply(SyntheticCreationalContext<TenantConfigBean> ctx) {
                final OidcImpl oidc = new OidcImpl(config);
                ctx.getInjectedReference(new TypeLiteral<Event<Oidc>>() {
                }).fire(oidc);
                return new TenantConfigBean(vertx.get(), registry.get(), oidc, securityConfig.events().enabled());
            }
        };
    }

    public void initTenantConfigBean() {
        try {
            // makes sure that config of static tenants is validated during app startup and create static tenant contexts
            Arc.container().instance(TenantConfigBean.class).get();
        } catch (CreationException wrapper) {
            if (wrapper.getCause() instanceof RuntimeException runtimeException) {
                // so that users see ConfigurationException etc. without noise
                throw runtimeException;
            }
            throw wrapper;
        }
    }

    public Function<String, Consumer<RoutingContext>> tenantResolverInterceptorCreator() {
        return new Function<String, Consumer<RoutingContext>>() {
            @Override
            public Consumer<RoutingContext> apply(String tenantId) {
                return new Consumer<RoutingContext>() {
                    @Override
                    public void accept(RoutingContext routingContext) {
                        OidcTenantConfig tenantConfig = routingContext.get(OidcTenantConfig.class.getName());
                        if (tenantConfig != null) {
                            // authentication has happened before @Tenant annotation was matched with the HTTP request
                            String tenantUsedForAuth = tenantConfig.tenantId().orElse(null);
                            if (tenantId.equals(tenantUsedForAuth)) {
                                // @Tenant selects the same tenant as already selected
                                return;
                            } else {
                                // @Tenant selects the different tenant than already selected
                                throw new AuthenticationFailedException(
                                        """
                                                The '%1$s' selected with the @Tenant annotation must be used to authenticate
                                                the request but it was already authenticated with the '%2$s' tenant. It
                                                can happen if the '%1$s' is selected with an annotation but '%2$s' is
                                                resolved during authentication required by the HTTP Security Policy which
                                                is enforced before the JAX-RS chain is run. In such cases, please set the
                                                'quarkus.http.auth.permission."permissions".applies-to=JAXRS' to all HTTP
                                                Security Policies which secure the same REST endpoints as the ones
                                                where the '%1$s' tenant is resolved by the '@Tenant' annotation.
                                                """
                                                .formatted(tenantId, tenantUsedForAuth));
                            }
                        }

                        LOG.debugf("@Tenant annotation set a '%s' tenant id on the %s request path", tenantId,
                                routingContext.request().path());
                        routingContext.put(OidcUtils.TENANT_ID_SET_BY_ANNOTATION, tenantId);
                        routingContext.put(OidcUtils.TENANT_ID_ATTRIBUTE, tenantId);
                    }
                };
            }
        };
    }

    public Supplier<TenantIdentityProvider> createTenantIdentityProvider(String tenantName) {
        return new Supplier<TenantIdentityProvider>() {
            @Override
            public TenantIdentityProvider get() {
                return new TenantSpecificOidcIdentityProvider(tenantName);
            }
        };
    }

    public Function<String, Consumer<RoutingContext>> authCtxInterceptorCreator() {
        return new Function<String, Consumer<RoutingContext>>() {
            @Override
            public Consumer<RoutingContext> apply(String acrValues) {
                OidcAuthenticationPolicy policy = OAuth2StepUpAuthenticationPolicy.create(acrValues);
                return new Consumer<RoutingContext>() {
                    @Override
                    public void accept(RoutingContext routingContext) {
                        String requestPath = routingContext.request().path();
                        OidcTenantConfig tenantConfig = routingContext.get(OidcTenantConfig.class.getName());
                        if (tenantConfig != null || routingContext.user() != null) {
                            throw new AuthenticationFailedException("""
                                    Authentication has happened before the '@AuthenticationContext' annotation was
                                    matched with the HTTP request path '%s'. It can happen when the authentication
                                    is required by an HTTP Security Policy before the JAX-RS chain is run. In such
                                    cases, please set the 'quarkus.http.auth.permission."permissions".applies-to=JAXRS'
                                    to all HTTP Security Policies which secure the same REST endpoints as the ones
                                    annotated with the '@AuthenticationContext' annotation.
                                    """.formatted(requestPath));
                        }
                        LOG.debugf("The '@AuthenticationContext' annotation set required 'acr' values for the '%s' request"
                                + " path to '%s'", requestPath, acrValues);
                        routingContext.put(AUTHENTICATION_POLICY_KEY, policy);
                    }
                };
            }
        };
    }

    private static final class TenantSpecificOidcIdentityProvider extends OidcIdentityProvider
            implements TenantIdentityProvider {

        private final String tenantId;
        private final BlockingSecurityExecutor blockingExecutor;

        private TenantSpecificOidcIdentityProvider(String tenantId) {
            super(Arc.container().instance(DefaultTenantConfigResolver.class).get(),
                    Arc.container().instance(BlockingSecurityExecutor.class).get());
            this.blockingExecutor = Arc.container().instance(BlockingSecurityExecutor.class).get();
            if (tenantId.equals(DEFAULT_TENANT_ID)) {
                OidcConfig config = Arc.container().instance(OidcConfig.class).get();
                this.tenantId = getDefaultTenant(config).tenantId().orElse(OidcUtils.DEFAULT_TENANT_ID);
            } else {
                this.tenantId = tenantId;
            }
        }

        @Override
        public Uni<SecurityIdentity> authenticate(AccessTokenCredential token) {
            return authenticate(new TokenAuthenticationRequest(token));
        }

        @Override
        protected Uni<TenantConfigContext> resolveTenantConfigContext(TokenAuthenticationRequest request,
                AuthenticationRequestContext context) {
            return tenantResolver.resolveContext(tenantId).onItem().ifNull().failWith(new Supplier<Throwable>() {
                @Override
                public Throwable get() {
                    return new OIDCException("Failed to resolve tenant context");
                }
            });
        }

        @Override
        protected Map<String, Object> getRequestData(TokenAuthenticationRequest request) {
            RoutingContext context = getRoutingContextAttribute(request);
            if (context != null) {
                return context.data();
            }
            return new HashMap<>();
        }

        private Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request) {
            return authenticate(request, new AuthenticationRequestContext() {
                @Override
                public Uni<SecurityIdentity> runBlocking(Supplier<SecurityIdentity> function) {
                    return blockingExecutor.executeBlocking(function);
                }
            });
        }
    }
}
