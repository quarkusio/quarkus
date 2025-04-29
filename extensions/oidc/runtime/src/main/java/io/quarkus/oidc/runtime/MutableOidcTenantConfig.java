package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcUtils.needsToSetDefaults;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfigBuilder;

/**
 * This internal class allows to adapt {@link io.quarkus.oidc.runtime.OidcTenantConfig} to information resolved
 * dynamically. For example, whether OIDC provider is available and supports the UserInfo endpoint.
 */
final class MutableOidcTenantConfig extends OidcTenantConfig {

    /**
     * Following fields are set dynamically, and we cannot resolve their value without information from OIDC provider.
     */
    private volatile boolean dynamicallyDisabledTenant;
    private final io.quarkus.oidc.runtime.OidcTenantConfig.Authentication authentication;
    private volatile boolean dynamicallyEnabledUserInfo;

    MutableOidcTenantConfig(io.quarkus.oidc.runtime.OidcTenantConfig mapping) {
        super(mapping);
        this.dynamicallyDisabledTenant = false;
        this.authentication = mapping.authentication();
        this.dynamicallyEnabledUserInfo = false;
    }

    void disableTenant() {
        dynamicallyDisabledTenant = true;
    }

    void setUserInfoRequired() {
        dynamicallyEnabledUserInfo = true;
    }

    @Override
    public boolean tenantEnabled() {
        if (dynamicallyDisabledTenant) {
            return false;
        }
        return super.tenantEnabled();
    }

    @Override
    public io.quarkus.oidc.runtime.OidcTenantConfig.Authentication authentication() {
        return new io.quarkus.oidc.runtime.OidcTenantConfig.Authentication() {
            public Optional<ResponseMode> responseMode() {
                return authentication.responseMode();
            }

            public Optional<String> redirectPath() {
                return authentication.redirectPath();
            }

            public boolean restorePathAfterRedirect() {
                return authentication.restorePathAfterRedirect();
            }

            public boolean removeRedirectParameters() {
                return authentication.removeRedirectParameters();
            }

            public Optional<String> errorPath() {
                return authentication.errorPath();
            }

            public Optional<String> sessionExpiredPath() {
                return authentication.sessionExpiredPath();
            }

            public boolean verifyAccessToken() {
                return authentication.verifyAccessToken();
            }

            public Optional<Boolean> forceRedirectHttpsScheme() {
                return authentication.forceRedirectHttpsScheme();
            }

            public Optional<List<String>> scopes() {
                return authentication.scopes();
            }

            public Optional<String> scopeSeparator() {
                return authentication.scopeSeparator();
            }

            public boolean nonceRequired() {
                return authentication.nonceRequired();
            }

            public Optional<Boolean> addOpenidScope() {
                return authentication.addOpenidScope();
            }

            public Map<String, String> extraParams() {
                return authentication.extraParams();
            }

            public Optional<List<String>> forwardParams() {
                return authentication.forwardParams();
            }

            public boolean cookieForceSecure() {
                return authentication.cookieForceSecure();
            }

            public Optional<String> cookieSuffix() {
                return authentication.cookieSuffix();
            }

            public String cookiePath() {
                return authentication.cookiePath();
            }

            public Optional<String> cookiePathHeader() {
                return authentication.cookiePathHeader();
            }

            public Optional<String> cookieDomain() {
                return authentication.cookieDomain();
            }

            public CookieSameSite cookieSameSite() {
                return authentication.cookieSameSite();
            }

            public boolean allowMultipleCodeFlows() {
                return authentication.allowMultipleCodeFlows();
            }

            public boolean failOnMissingStateParam() {
                return authentication.failOnMissingStateParam();
            }

            public boolean failOnUnresolvedKid() {
                return authentication.failOnUnresolvedKid();
            }

            public Optional<Boolean> userInfoRequired() {
                if (dynamicallyEnabledUserInfo) {
                    return Optional.of(true);
                }
                return authentication.userInfoRequired();
            }

            public Duration sessionAgeExtension() {
                return authentication.sessionAgeExtension();
            }

            public Duration stateCookieAge() {
                return authentication.stateCookieAge();
            }

            public boolean javaScriptAutoRedirect() {
                return authentication.javaScriptAutoRedirect();
            }

            public Optional<Boolean> idTokenRequired() {
                return authentication.idTokenRequired();
            }

            public Optional<Duration> internalIdTokenLifespan() {
                return authentication.internalIdTokenLifespan();
            }

            public Optional<Boolean> pkceRequired() {
                return authentication.pkceRequired();
            }

            public Optional<String> pkceSecret() {
                return authentication.pkceSecret();
            }

            public Optional<String> stateSecret() {
                return authentication.stateSecret();
            }
        };
    }

    /**
     * Presets documented defaults if they are not applied already.
     */
    static OidcTenantConfig recreateIfNecessary(io.quarkus.oidc.runtime.OidcTenantConfig tenantConfig) {
        if (tenantConfig == null) {
            return null;
        }
        if (tenantConfig instanceof MutableOidcTenantConfig mutableTenantConfig) {
            return mutableTenantConfig;
        }

        // at this point, the 'tenantConfig' is either created by SmallRye Config or by users with deprecated constructor
        if (needsToSetDefaults(tenantConfig.provider(), tenantConfig.applicationType(), tenantConfig.token(),
                tenantConfig.roles(), tenantConfig.authentication())) {
            return new OidcTenantConfigBuilder(tenantConfig).build();
        }

        if (tenantConfig instanceof OidcTenantConfig) {
            // created by legacy constructor without the tenant builder, so let's make sure that defaults are correct
            // TODO: drop this when deprecated OidcTenantConfig setters and fields are removed
            return new OidcTenantConfigBuilder(tenantConfig).build();
        }

        // created by SmallRye Config and the instance doesn't need any changes
        return new MutableOidcTenantConfig(tenantConfig);
    }
}
