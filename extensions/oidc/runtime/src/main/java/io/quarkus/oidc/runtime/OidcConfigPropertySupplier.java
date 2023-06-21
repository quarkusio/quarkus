package io.quarkus.oidc.runtime;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Provider;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.providers.KnownOidcProviders;

public class OidcConfigPropertySupplier implements Supplier<String> {
    private static final String AUTH_SERVER_URL_CONFIG_KEY = "quarkus.oidc.auth-server-url";
    private static final String END_SESSION_PATH_CONFIG_KEY = "quarkus.oidc.end-session-path";
    private static final String TOKEN_PATH_CONFIG_KEY = "quarkus.oidc.token-path";
    private static final String AUTH_PATH_CONFIG_KEY = "quarkus.oidc.authorization-path";
    private static final Set<String> RELATIVE_PATH_CONFIG_PROPS = Set.of(END_SESSION_PATH_CONFIG_KEY,
            TOKEN_PATH_CONFIG_KEY, AUTH_PATH_CONFIG_KEY);
    private static final String OIDC_PROVIDER_CONFIG_KEY = "quarkus.oidc.provider";
    private static final String SCOPES_KEY = "quarkus.oidc.authentication.scopes";
    private String oidcConfigProperty;
    private String defaultValue;
    private boolean urlProperty;

    public OidcConfigPropertySupplier() {

    }

    public OidcConfigPropertySupplier(String oidcConfigProperty) {
        this(oidcConfigProperty, null);
    }

    public OidcConfigPropertySupplier(String oidcConfigProperty, String defaultValue) {
        this(oidcConfigProperty, defaultValue, false);
    }

    public OidcConfigPropertySupplier(String oidcConfigProperty, String defaultValue, boolean urlProperty) {
        this.oidcConfigProperty = oidcConfigProperty;
        this.defaultValue = defaultValue;
        this.urlProperty = urlProperty;
    }

    @Override
    public String get() {
        return get(ConfigProvider.getConfig());
    }

    private String checkUrlProperty(Optional<String> value, OidcTenantConfig providerConfig, Config config) {
        if (urlProperty && value.isPresent() && !value.get().startsWith("http:")) {
            Optional<String> authServerUrl = config.getOptionalValue(AUTH_SERVER_URL_CONFIG_KEY,
                    String.class);
            if (authServerUrl.isEmpty() && providerConfig != null) {
                authServerUrl = providerConfig.authServerUrl;
            }
            return authServerUrl.isPresent() ? OidcCommonUtils.getOidcEndpointUrl(authServerUrl.get(), value) : null;
        }
        return value.orElse(null);
    }

    public String getOidcConfigProperty() {
        return oidcConfigProperty;
    }

    public void setOidcConfigProperty(String oidcConfigProperty) {
        this.oidcConfigProperty = oidcConfigProperty;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isUrlProperty() {
        return urlProperty;
    }

    public void setUrlProperty(boolean urlProperty) {
        this.urlProperty = urlProperty;
    }

    public String get(Config config) {
        Optional<Provider> provider = config.getOptionalValue(OIDC_PROVIDER_CONFIG_KEY,
                Provider.class);
        OidcTenantConfig providerConfig = provider.isPresent() ? KnownOidcProviders.provider(provider.get()) : null;
        if (defaultValue != null || RELATIVE_PATH_CONFIG_PROPS.contains(oidcConfigProperty)) {
            Optional<String> value = config.getOptionalValue(oidcConfigProperty, String.class);
            if (value.isEmpty() && providerConfig != null) {
                if (END_SESSION_PATH_CONFIG_KEY.equals(oidcConfigProperty)) {
                    value = providerConfig.endSessionPath;
                } else if (TOKEN_PATH_CONFIG_KEY.equals(oidcConfigProperty)) {
                    value = providerConfig.tokenPath;
                } else if (AUTH_PATH_CONFIG_KEY.equals(oidcConfigProperty)) {
                    value = providerConfig.authorizationPath;
                }
            }
            if (value.isPresent()) {
                return checkUrlProperty(value, providerConfig, config);
            }
            return defaultValue;
        } else if (SCOPES_KEY.equals(oidcConfigProperty)) {
            Optional<List<String>> scopes = config.getOptionalValues(oidcConfigProperty, String.class);
            if (scopes.isEmpty() && providerConfig != null) {
                scopes = providerConfig.authentication.scopes;
            }
            if (scopes.isPresent()) {
                return OidcCommonUtils.urlEncode(String.join(" ", scopes.get()));
            } else {
                return OidcConstants.OPENID_SCOPE;
            }
        } else {
            return checkUrlProperty(config.getOptionalValue(oidcConfigProperty, String.class),
                    providerConfig, config);
        }
    }
}
