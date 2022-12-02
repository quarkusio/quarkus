package io.quarkus.oidc.runtime.providers;

import java.util.List;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Authentication.ResponseMode;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials.Secret.Method;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;

public class KnownOidcProviders {

    public static OidcTenantConfig provider(OidcTenantConfig.Provider provider) {
        if (OidcTenantConfig.Provider.GITHUB == provider) {
            return github();
        } else if (OidcTenantConfig.Provider.GOOGLE == provider) {
            return google();
        } else if (OidcTenantConfig.Provider.APPLE == provider) {
            return apple();
        } else if (OidcTenantConfig.Provider.MICROSOFT == provider) {
            return microsoft();
        } else if (OidcTenantConfig.Provider.FACEBOOK == provider) {
            return facebook();
        } else if (OidcTenantConfig.Provider.SPOTIFY == provider) {
            return spotify();
        } else if (OidcTenantConfig.Provider.TWITTER == provider) {
            return twitter();
        }
        return null;
    }

    private static OidcTenantConfig github() {
        OidcTenantConfig ret = new OidcTenantConfig();
        ret.setAuthServerUrl("https://github.com/login/oauth");
        ret.setApplicationType(OidcTenantConfig.ApplicationType.WEB_APP);
        ret.setDiscoveryEnabled(false);
        ret.setAuthorizationPath("authorize");
        ret.setTokenPath("access_token");
        ret.setUserInfoPath("https://api.github.com/user");
        ret.getAuthentication().setScopes(List.of("user:email"));
        ret.getAuthentication().setUserInfoRequired(true);
        ret.getAuthentication().setIdTokenRequired(false);
        return ret;
    }

    private static OidcTenantConfig twitter() {
        OidcTenantConfig ret = new OidcTenantConfig();
        ret.setAuthServerUrl("https://api.twitter.com/2/oauth2");
        ret.setApplicationType(OidcTenantConfig.ApplicationType.WEB_APP);
        ret.setDiscoveryEnabled(false);
        ret.setAuthorizationPath("https://twitter.com/i/oauth2/authorize");
        ret.setTokenPath("token");
        ret.setUserInfoPath("https://api.twitter.com/2/users/me");
        ret.getAuthentication().setAddOpenidScope(false);
        ret.getAuthentication().setScopes(List.of("offline.access", "tweet.read", "users.read"));
        ret.getAuthentication().setUserInfoRequired(true);
        ret.getAuthentication().setIdTokenRequired(false);
        ret.getAuthentication().setPkceRequired(true);
        return ret;
    }

    private static OidcTenantConfig google() {
        OidcTenantConfig ret = new OidcTenantConfig();
        ret.setAuthServerUrl("https://accounts.google.com");
        ret.setApplicationType(OidcTenantConfig.ApplicationType.WEB_APP);
        ret.getAuthentication().setScopes(List.of("openid", "email", "profile"));
        return ret;
    }

    private static OidcTenantConfig microsoft() {
        OidcTenantConfig ret = new OidcTenantConfig();
        ret.setAuthServerUrl("https://login.microsoftonline.com/common/v2.0");
        ret.setApplicationType(OidcTenantConfig.ApplicationType.WEB_APP);
        ret.getToken().setIssuer("any");
        ret.getAuthentication().setScopes(List.of("openid", "email", "profile"));
        return ret;
    }

    private static OidcTenantConfig facebook() {
        OidcTenantConfig ret = new OidcTenantConfig();
        ret.setAuthServerUrl("https://www.facebook.com");
        ret.setApplicationType(OidcTenantConfig.ApplicationType.WEB_APP);
        ret.setDiscoveryEnabled(false);
        ret.setAuthorizationPath("https://facebook.com/dialog/oauth/");
        ret.setTokenPath("https://graph.facebook.com/v12.0/oauth/access_token");
        ret.setJwksPath("https://www.facebook.com/.well-known/oauth/openid/jwks/");
        ret.getAuthentication().setScopes(List.of("email", "public_profile"));
        ret.getAuthentication().setForceRedirectHttpsScheme(true);
        return ret;
    }

    private static OidcTenantConfig apple() {
        OidcTenantConfig ret = new OidcTenantConfig();
        ret.setAuthServerUrl("https://appleid.apple.com/");
        ret.setApplicationType(OidcTenantConfig.ApplicationType.WEB_APP);
        ret.getAuthentication().setScopes(List.of("openid", "email", "name"));
        ret.getAuthentication().setForceRedirectHttpsScheme(true);
        ret.getAuthentication().setResponseMode(ResponseMode.FORM_POST);
        ret.getCredentials().getClientSecret().setMethod(Method.POST_JWT);
        ret.getCredentials().getJwt().setSignatureAlgorithm(SignatureAlgorithm.ES256.getAlgorithm());
        ret.getCredentials().getJwt().setAudience("https://appleid.apple.com/");
        return ret;
    }

    private static OidcTenantConfig spotify() {
        // See https://developer.spotify.com/documentation/general/guides/authorization/code-flow/
        OidcTenantConfig ret = new OidcTenantConfig();
        ret.setDiscoveryEnabled(false);
        ret.setAuthServerUrl("https://accounts.spotify.com");
        ret.setApplicationType(OidcTenantConfig.ApplicationType.WEB_APP);
        ret.setAuthorizationPath("authorize");
        ret.setTokenPath("api/token");
        ret.setUserInfoPath("https://api.spotify.com/v1/me");

        OidcTenantConfig.Authentication authentication = ret.getAuthentication();
        authentication.setAddOpenidScope(false);
        authentication.setScopes(List.of("user-read-email"));
        authentication.setUserInfoRequired(true);
        authentication.setIdTokenRequired(false);
        authentication.setPkceRequired(true);

        return ret;
    }
}
