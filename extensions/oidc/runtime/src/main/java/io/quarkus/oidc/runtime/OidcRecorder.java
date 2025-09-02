package io.quarkus.oidc.runtime;

import static io.quarkus.runtime.configuration.DurationConverter.parseDuration;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.util.TypeLiteral;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.oidc.Oidc;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantIdentityProvider;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RuntimeInit;
import io.quarkus.runtime.annotations.StaticInit;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.runtime.SecurityConfig;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class OidcRecorder {
    public static final String ACR_VALUES_TO_MAX_AGE_SEPARATOR = "@#$%@";

    static final Logger LOG = Logger.getLogger(OidcRecorder.class);

    private final RuntimeValue<OidcConfig> oidcConfig;
    private final RuntimeValue<SecurityConfig> securityConfig;

    public OidcRecorder(final RuntimeValue<OidcConfig> oidcConfig, final RuntimeValue<SecurityConfig> securityConfig) {
        this.oidcConfig = oidcConfig;
        this.securityConfig = securityConfig;
    }

    public Supplier<DefaultTokenIntrospectionUserInfoCache> setupTokenCache(Supplier<Vertx> vertx) {
        return new Supplier<DefaultTokenIntrospectionUserInfoCache>() {
            @Override
            public DefaultTokenIntrospectionUserInfoCache get() {
                return new DefaultTokenIntrospectionUserInfoCache(oidcConfig.getValue(), vertx.get());
            }
        };
    }

    @StaticInit
    public void setUserInfoInjectionPointDetected(boolean userInfoInjectionPointDetected) {
        TenantContextFactory.userInfoInjectionPointDetected = userInfoInjectionPointDetected;
    }

    @RuntimeInit
    public Function<SyntheticCreationalContext<TenantConfigBean>, TenantConfigBean> createTenantConfigBean(
            Supplier<Vertx> vertx, Supplier<TlsConfigurationRegistry> registry) {
        return new Function<SyntheticCreationalContext<TenantConfigBean>, TenantConfigBean>() {
            @Override
            public TenantConfigBean apply(SyntheticCreationalContext<TenantConfigBean> ctx) {
                final OidcImpl oidc = new OidcImpl(oidcConfig.getValue());
                ctx.getInjectedReference(new TypeLiteral<Event<Oidc>>() {
                }).fire(oidc);
                return new TenantConfigBean(vertx.get(), registry.get(), oidc, securityConfig.getValue().events().enabled());
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

    public Function<String, Consumer<RoutingContext>> authenticationContextInterceptorCreator() {
        StepUpAuthenticationPolicy.markAsEnabled();
        return new Function<String, Consumer<RoutingContext>>() {
            @Override
            public Consumer<RoutingContext> apply(String annotationBinding) {
                int separatorIndex = annotationBinding.indexOf(ACR_VALUES_TO_MAX_AGE_SEPARATOR);
                String acrValues = annotationBinding.substring(0, separatorIndex);
                String maxAgeAsStr = annotationBinding.substring(separatorIndex + ACR_VALUES_TO_MAX_AGE_SEPARATOR.length());
                final Duration maxAgeDuration;
                if (maxAgeAsStr.isEmpty()) {
                    maxAgeDuration = null;
                } else {
                    maxAgeDuration = parseDuration(maxAgeAsStr);
                }
                StepUpAuthenticationPolicy policy = new StepUpAuthenticationPolicy(acrValues, maxAgeDuration);
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
                        LOG.debugf("The '@AuthenticationContext' annotation set required 'acr' values '%s' "
                                + "and max age '%s' for the request path '%s'", acrValues, maxAgeAsStr, requestPath);
                        policy.storeSelfOnContext(routingContext);
                    }
                };
            }
        };
    }

    public Handler<RoutingContext> getBackChannelLogoutHandler(BeanContainer beanContainer) {
        return beanContainer.beanInstance(BackChannelLogoutHandler.class);
    }

    public Handler<RoutingContext> getResourceMetadataHandler(BeanContainer beanContainer) {
        return beanContainer.beanInstance(ResourceMetadataHandler.class);
    }

}
