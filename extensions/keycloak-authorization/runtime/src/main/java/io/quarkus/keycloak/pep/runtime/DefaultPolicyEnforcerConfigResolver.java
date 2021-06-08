package io.quarkus.keycloak.pep.runtime;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.keycloak.pep.PolicyEnforcerConfigResolver;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class DefaultPolicyEnforcerConfigResolver {

    private static final Logger LOG = Logger.getLogger(DefaultPolicyEnforcerConfigResolver.class);
    private static final String IDENTITY_TENANT_ID_ATTRIBUTE = "tenant-id";
    private static final String CURRENT_DYNAMIC_TENANT_ENFORCER_CONFIG = "dynamic.pep.tenant.config";
    private static final String CURRENT_DYNAMIC_TENANT_ENFORCER_CONFIG_NULL = "dynamic.pep.tenant.config.null";
    private static final String STATIC_TENANT_ID_ATTRIBUTE = "static.tenant.id";
    private static final String CURRENT_DYNAMIC_OIDC_CONFIG = "dynamic.tenant.config";

    @Inject
    Instance<PolicyEnforcerConfigResolver> policyEnforcerConfigResolver;

    @Inject
    KeycloakPolicyEnforcerConfigBean tenantConfigBean;

    @PostConstruct
    public void verifyResolvers() {
        if (policyEnforcerConfigResolver.isResolvable() && policyEnforcerConfigResolver.isAmbiguous()) {
            throw new IllegalStateException("Multiple " + PolicyEnforcerConfigResolver.class + " beans registered");
        }
    }

    Uni<KeycloakPolicyEnforcerContext> resolveContext(RoutingContext context) {
        if (context == null) {
            return Uni.createFrom().item(tenantConfigBean.getDefaultPolicyConfig());
        }
        Uni<KeycloakPolicyEnforcerContext> uniEnforcerContext = getDynamicPolicyEnforcerContext(context);
        if (uniEnforcerContext != null) {
            return uniEnforcerContext;
        }
        KeycloakPolicyEnforcerContext tenantContext = getStaticPolicyEnforcerContext(context);
        if (tenantContext != null && !tenantContext.ready) {
            String tenantId = getTenantId(context);

            // check if it the connection has already been created
            KeycloakPolicyEnforcerContext readyEnforcerContext = tenantConfigBean.getDynamicPolicyConfig()
                    .get(tenantId);
            if (readyEnforcerContext == null) {
                LOG.debugf("Enforcer for tenant '%s' is not initialized yet, trying to create OIDC connection now",
                        tenantId);
                return tenantConfigBean.getPolicyConfigContextFactory().apply(tenantContext.oidcTenantConfig,
                        tenantContext.enforcerConfig);
            } else {
                tenantContext = readyEnforcerContext;
            }
        }

        return Uni.createFrom().item(tenantContext);
    }

    KeycloakPolicyEnforcerContext resolveIdentityContext(SecurityIdentity identity) {
        String tenantId = identity.getAttribute(IDENTITY_TENANT_ID_ATTRIBUTE);
        KeycloakPolicyEnforcerContext dynamicContext = tenantConfigBean.getDynamicPolicyConfig().get(tenantId);
        if (dynamicContext != null) {
            return dynamicContext;
        }
        return tenantConfigBean.getDefaultPolicyConfig();
    }

    private KeycloakPolicyEnforcerTenantConfig getDynamicPolicyEnforcerConfig(RoutingContext context) {
        KeycloakPolicyEnforcerTenantConfig enforcerConfig = null;
        if (policyEnforcerConfigResolver.isResolvable()) {
            enforcerConfig = context.get(CURRENT_DYNAMIC_TENANT_ENFORCER_CONFIG);
            if (enforcerConfig == null && context.get(CURRENT_DYNAMIC_TENANT_ENFORCER_CONFIG_NULL) == null) {
                enforcerConfig = policyEnforcerConfigResolver.get().resolve(context);
                if (enforcerConfig != null) {
                    context.put(CURRENT_DYNAMIC_TENANT_ENFORCER_CONFIG, enforcerConfig);
                } else {
                    context.put(CURRENT_DYNAMIC_TENANT_ENFORCER_CONFIG_NULL, true);
                }
            }
        }
        return enforcerConfig;
    }

    private Uni<KeycloakPolicyEnforcerContext> getDynamicPolicyEnforcerContext(RoutingContext context) {

        KeycloakPolicyEnforcerTenantConfig enforcerConfig = getDynamicPolicyEnforcerConfig(context);
        if (enforcerConfig != null) {
            String tenantId = getTenantId(context);
            KeycloakPolicyEnforcerContext enforcerContext = tenantConfigBean.getDynamicPolicyConfig().get(tenantId);

            if (enforcerContext == null) {
                OidcTenantConfig dynamicOidcConfig = context.get(CURRENT_DYNAMIC_OIDC_CONFIG);
                if (dynamicOidcConfig == null) {
                    dynamicOidcConfig = tenantConfigBean.getTenantOidcConfig(tenantId);
                }
                // If no config found, use default OIDC config
                if (dynamicOidcConfig == null) {
                    dynamicOidcConfig = tenantConfigBean.getDefaultPolicyConfig().oidcTenantConfig;
                }
                return tenantConfigBean.getPolicyConfigContextFactory().apply(dynamicOidcConfig, enforcerConfig);
            } else {
                return Uni.createFrom().item(enforcerContext);
            }
        }

        return null;
    }

    private KeycloakPolicyEnforcerContext getStaticPolicyEnforcerContext(RoutingContext context) {

        String tenantId = getTenantId(context);

        KeycloakPolicyEnforcerContext configContext = tenantId != null
                ? tenantConfigBean.getStaticPolicyConfig().get(tenantId)
                : null;
        if (configContext == null) {
            if (tenantId != null && !tenantId.isEmpty()) {
                LOG.debugf(
                        "Registered PolicyResolver has not provided the configuration for tenant '%s', using the default tenant",
                        tenantId);
            }
            configContext = tenantConfigBean.getDefaultPolicyConfig();
        }
        return configContext;
    }

    private String getTenantId(RoutingContext context) {
        OidcTenantConfig dynamicOidcConfig = context.get(CURRENT_DYNAMIC_OIDC_CONFIG);
        String tenantId = dynamicOidcConfig != null ? dynamicOidcConfig.getTenantId().get()
                : context.get(STATIC_TENANT_ID_ATTRIBUTE);
        return tenantId;
    }
}
