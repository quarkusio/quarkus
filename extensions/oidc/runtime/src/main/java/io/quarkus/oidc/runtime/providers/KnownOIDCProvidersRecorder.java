package io.quarkus.oidc.runtime.providers;

import java.util.Optional;
import java.util.function.Supplier;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KnownOIDCProvidersRecorder {

    final OidcConfig config;

    public KnownOIDCProvidersRecorder(OidcConfig config) {
        this.config = config;
    }

    public Supplier<Optional<OidcTenantConfig>> github() {
        return new Supplier<Optional<OidcTenantConfig>>() {
            @Override
            public Optional<OidcTenantConfig> get() {
                if (config.provider.github.isEmpty()) {
                    return Optional.empty();
                }
                OidcTenantConfig ret = new OidcTenantConfig();
                ret.clientId = Optional.of(config.provider.github.get().clientId);
                ret.credentials.secret = Optional.of(config.provider.github.get().secret);
                ret.authServerUrl = Optional.of("https://github.com/login/oauth");
                //TODO: do we want to hard code this?
                ret.applicationType = OidcTenantConfig.ApplicationType.HYBRID;
                ret.discoveryEnabled = false;
                ret.authorizationPath = Optional.of("authorize");
                ret.tokenPath = Optional.of("access_token");
                ret.userInfoPath = Optional.of("https://api.github.com/user");
                ret.authentication.scopes = Optional.of(config.provider.github.get().scopes);
                ret.authentication.userInfoRequired = true;
                ret.authentication.setIdTokenRequired(false);
                ret.authentication.setRedirectPath("/Login/githubLoginSuccess");
                return Optional.of(ret);
            }
        };
    }

    public Supplier<Optional<OidcTenantConfig>> google() {
        return new Supplier<Optional<OidcTenantConfig>>() {
            @Override
            public Optional<OidcTenantConfig> get() {
                if (config.provider.google.isEmpty()) {
                    return Optional.empty();
                }
                OidcTenantConfig ret = new OidcTenantConfig();
                ret.clientId = Optional.of(config.provider.google.get().clientId);
                ret.credentials.secret = Optional.of(config.provider.google.get().secret);
                ret.authServerUrl = Optional.of("https://accounts.google.com");
                //TODO: do we want to hard code this?
                ret.applicationType = OidcTenantConfig.ApplicationType.HYBRID;
                ret.authentication.scopes = Optional.of(config.provider.google.get().scopes);
                ret.authentication.setRedirectPath("/Login/oidcLoginSuccess");
                return Optional.of(ret);
            }
        };
    }

    public Supplier<Optional<OidcTenantConfig>> microsoft() {
        return new Supplier<Optional<OidcTenantConfig>>() {
            @Override
            public Optional<OidcTenantConfig> get() {
                if (config.provider.microsoft.isEmpty()) {
                    return Optional.empty();
                }
                OidcTenantConfig ret = new OidcTenantConfig();
                ret.clientId = Optional.of(config.provider.microsoft.get().clientId);
                ret.credentials.secret = Optional.of(config.provider.microsoft.get().secret);
                ret.authServerUrl = Optional.of("https://login.microsoftonline.com/common/v2.0");
                //TODO: do we want to hard code this?
                ret.applicationType = OidcTenantConfig.ApplicationType.HYBRID;
                ret.authentication.setRedirectPath("/Login/oidcLoginSuccess");
                ret.token.setIssuer("any");
                return Optional.of(ret);
            }
        };
    }

    public Supplier<Optional<OidcTenantConfig>> facebook() {
        return new Supplier<Optional<OidcTenantConfig>>() {
            @Override
            public Optional<OidcTenantConfig> get() {
                if (config.provider.facebook.isEmpty()) {
                    return Optional.empty();
                }
                OidcTenantConfig ret = new OidcTenantConfig();
                ret.clientId = Optional.of(config.provider.facebook.get().clientId);
                ret.credentials.secret = Optional.of(config.provider.facebook.get().secret);
                ret.authServerUrl = Optional.of("https://www.facebook.com");
                ret.authentication.scopes = Optional.of(config.provider.facebook.get().scopes);
                ret.applicationType = OidcTenantConfig.ApplicationType.HYBRID;
                ret.authentication.setRedirectPath("/Login/facebookLoginSuccess");
                ret.discoveryEnabled = false;
                ret.tokenPath = Optional.of("https://graph.facebook.com/v12.0/oauth/access_token");
                ret.token.setIssuer("facebook");
                ret.setAuthorizationPath("https://facebook.com/dialog/oauth/");
                ret.setJwksPath("https://www.facebook.com/.well-known/oauth/openid/jwks/");
                ret.setUserInfoPath("https://graph.facebook.com/me/?fields=" + config.provider.facebook.get().userInfoFields);
                ret.authentication.setUserInfoRequired(true);
                ret.authentication.setIdTokenRequired(false);
                return Optional.of(ret);
            }
        };
    }
}
