package io.quarkus.oidc.runtime.providers;

import java.util.List;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Authentication.ResponseMode;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials.Secret.Method;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;

public class KnownOidcProviders {

    public static OidcTenantConfig provider(OidcTenantConfig.Provider provider) {
        return switch (provider) {
            case APPLE -> apple();
            case DISCORD -> discord();
            case FACEBOOK -> facebook();
            case GITHUB -> github();
            case GOOGLE -> google();
            case LINKEDIN -> linkedIn();
            case MASTODON -> mastodon();
            case MICROSOFT -> microsoft();
            case SPOTIFY -> spotify();
            case STRAVA -> strava();
            case TWITCH -> twitch();
            case TWITTER, X -> twitter();
        };
    }

    private static OidcTenantConfig linkedIn() {
        OidcTenantConfig ret = new OidcTenantConfig();
        ret.setAuthServerUrl("https://www.linkedin.com/oauth");
        ret.setApplicationType(OidcTenantConfig.ApplicationType.WEB_APP);
        ret.getAuthentication().setScopes(List.of("email", "profile"));
        ret.getCredentials().getClientSecret().setMethod(Method.POST);
        ret.getToken().setPrincipalClaim("name");
        return ret;
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
        ret.getToken().setVerifyAccessTokenWithUserInfo(true);
        ret.getToken().setPrincipalClaim("name");
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
        ret.getToken().setPrincipalClaim("name");
        ret.getToken().setVerifyAccessTokenWithUserInfo(true);
        return ret;
    }

    private static OidcTenantConfig mastodon() {
        OidcTenantConfig ret = new OidcTenantConfig();
        ret.setDiscoveryEnabled(false);
        ret.setAuthServerUrl("https://mastodon.social");
        ret.setApplicationType(OidcTenantConfig.ApplicationType.WEB_APP);
        ret.setAuthorizationPath("/oauth/authorize");
        ret.setTokenPath("/oauth/token");

        ret.setUserInfoPath("/api/v1/accounts/verify_credentials");

        OidcTenantConfig.Authentication authentication = ret.getAuthentication();
        authentication.setAddOpenidScope(false);
        authentication.setScopes(List.of("read"));
        authentication.setUserInfoRequired(true);
        authentication.setIdTokenRequired(false);

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
        authentication.setScopes(List.of("user-read-private", "user-read-email"));
        authentication.setIdTokenRequired(false);
        authentication.setPkceRequired(true);

        ret.getToken().setVerifyAccessTokenWithUserInfo(true);
        ret.getToken().setPrincipalClaim("display_name");

        return ret;
    }

    private static OidcTenantConfig strava() {
        OidcTenantConfig ret = new OidcTenantConfig();
        ret.setDiscoveryEnabled(false);
        ret.setAuthServerUrl("https://www.strava.com/oauth");
        ret.setApplicationType(OidcTenantConfig.ApplicationType.WEB_APP);
        ret.setAuthorizationPath("authorize");

        ret.setTokenPath("token");
        ret.setUserInfoPath("https://www.strava.com/api/v3/athlete");

        OidcTenantConfig.Authentication authentication = ret.getAuthentication();
        authentication.setAddOpenidScope(false);
        authentication.setScopes(List.of("activity:read"));
        authentication.setIdTokenRequired(false);
        authentication.setRedirectPath("/strava");

        ret.getToken().setVerifyAccessTokenWithUserInfo(true);
        ret.getCredentials().getClientSecret().setMethod(Method.QUERY);

        return ret;
    }

    private static OidcTenantConfig twitch() {
        // Ref https://dev.twitch.tv/docs/authentication/getting-tokens-oidc/#oidc-authorization-code-grant-flow

        OidcTenantConfig ret = new OidcTenantConfig();
        ret.setAuthServerUrl("https://id.twitch.tv/oauth2");
        ret.setApplicationType(OidcTenantConfig.ApplicationType.WEB_APP);
        ret.getAuthentication().setForceRedirectHttpsScheme(true);
        ret.getCredentials().getClientSecret().setMethod(Method.POST);
        return ret;
    }

    private static OidcTenantConfig discord() {
        // Ref https://discord.com/developers/docs/topics/oauth2
        OidcTenantConfig ret = new OidcTenantConfig();
        ret.setAuthServerUrl("https://discord.com/api/oauth2");
        ret.setDiscoveryEnabled(false);
        ret.setAuthorizationPath("authorize");
        ret.setTokenPath("token");
        ret.getAuthentication().setScopes(List.of("identify", "email"));
        ret.getAuthentication().setIdTokenRequired(false);
        ret.getToken().setVerifyAccessTokenWithUserInfo(true);
        ret.setUserInfoPath("https://discord.com/api/users/@me");
        return ret;
    }
}
