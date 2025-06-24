package io.quarkus.oidc.runtime.providers;

import static io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Secret.Method.POST;
import static io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Secret.Method.POST_JWT;
import static io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Secret.Method.QUERY;
import static io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType.WEB_APP;
import static io.quarkus.oidc.runtime.OidcTenantConfig.Authentication.ResponseMode.FORM_POST;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig.Provider;
import io.quarkus.oidc.runtime.builders.AuthenticationConfigBuilder;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;

public class KnownOidcProviders {

    public static OidcTenantConfig provider(Provider provider) {
        return switch (provider) {
            case APPLE -> apple();
            case DISCORD -> discord();
            case FACEBOOK -> facebook();
            case GITHUB -> github();
            case GOOGLE -> google();
            case LINKEDIN -> linkedIn();
            case MASTODON -> mastodon();
            case MICROSOFT -> microsoft();
            case SLACK -> slack();
            case SPOTIFY -> spotify();
            case STRAVA -> strava();
            case TWITCH -> twitch();
            case TWITTER, X -> twitter();
        };
    }

    private static OidcTenantConfig slack() {
        return OidcTenantConfig
                .authServerUrl("https://slack.com")
                .applicationType(WEB_APP)
                .token("name")
                .authentication()
                .forceRedirectHttpsScheme()
                .scopes("profile", "email")
                .end()
                .build();
    }

    private static OidcTenantConfig linkedIn() {
        return OidcTenantConfig
                .authServerUrl("https://www.linkedin.com/oauth")
                .applicationType(WEB_APP)
                .authentication().scopes("email", "profile").end()
                .token().principalClaim("name").end()
                .credentials().clientSecret().method(POST).endCredentials()
                .build();
    }

    private static OidcTenantConfig github() {
        var authBuilder = new AuthenticationConfigBuilder();
        authBuilder.idTokenRequired(false);
        authBuilder.userInfoRequired();
        authBuilder.scopes("user:email");
        return OidcTenantConfig
                .authServerUrl("https://github.com/login/oauth")
                .applicationType(WEB_APP)
                .discoveryEnabled(false)
                .authorizationPath("authorize")
                .tokenPath("access_token")
                .userInfoPath("https://api.github.com/user")
                .token().verifyAccessTokenWithUserInfo(true).principalClaim("name").end()
                .authentication(authBuilder.build())
                .build();
    }

    private static OidcTenantConfig twitter() {
        var auth = new AuthenticationConfigBuilder()
                .addOpenidScope(false)
                .userInfoRequired()
                .idTokenRequired(false)
                .pkceRequired()
                .scopes("offline.access", "tweet.read", "users.read")
                .build();
        return OidcTenantConfig
                .authServerUrl("https://api.twitter.com/2/oauth2")
                .applicationType(WEB_APP)
                .discoveryEnabled(false)
                .authorizationPath("https://twitter.com/i/oauth2/authorize")
                .tokenPath("token")
                .userInfoPath("https://api.twitter.com/2/users/me")
                .authentication(auth)
                .build();
    }

    private static OidcTenantConfig google() {
        return OidcTenantConfig
                .authServerUrl("https://accounts.google.com")
                .applicationType(WEB_APP)
                .authentication().scopes("openid", "email", "profile").end()
                .token().verifyAccessTokenWithUserInfo(true).principalClaim("name").end()
                .build();
    }

    private static OidcTenantConfig mastodon() {
        var auth = new AuthenticationConfigBuilder()
                .addOpenidScope(false)
                .userInfoRequired()
                .idTokenRequired(false)
                .scopes("read")
                .build();
        return OidcTenantConfig
                .authServerUrl("https://mastodon.social")
                .discoveryEnabled(false)
                .applicationType(WEB_APP)
                .authorizationPath("/oauth/authorize")
                .tokenPath("/oauth/token")
                .userInfoPath("/api/v1/accounts/verify_credentials")
                .authentication(auth)
                .build();
    }

    private static OidcTenantConfig microsoft() {
        return OidcTenantConfig
                .authServerUrl("https://login.microsoftonline.com/common/v2.0")
                .applicationType(WEB_APP)
                .token().issuer("any").end()
                .authentication().scopes("openid", "email", "profile").end()
                .build();
    }

    private static OidcTenantConfig facebook() {
        return OidcTenantConfig
                .authServerUrl("https://www.facebook.com")
                .applicationType(WEB_APP)
                .discoveryEnabled(false)
                .authorizationPath("https://facebook.com/dialog/oauth/")
                .tokenPath("https://graph.facebook.com/v12.0/oauth/access_token")
                .jwksPath("https://www.facebook.com/.well-known/oauth/openid/jwks/")
                .authentication().scopes("email", "public_profile").forceRedirectHttpsScheme().end()
                .build();
    }

    private static OidcTenantConfig apple() {
        var builder = OidcTenantConfig.authServerUrl("https://appleid.apple.com/").applicationType(WEB_APP);

        builder.authentication()
                .scopes("openid", "email", "name")
                .forceRedirectHttpsScheme()
                .responseMode(FORM_POST)
                .end();

        builder.credentials()
                .jwt()
                .audience("https://appleid.apple.com/")
                .signatureAlgorithm(SignatureAlgorithm.ES256.getAlgorithm())
                .end()
                .clientSecret().method(POST_JWT)
                .endCredentials();

        return builder.build();
    }

    private static OidcTenantConfig spotify() {
        // See https://developer.spotify.com/documentation/general/guides/authorization/code-flow/
        var authentication = new AuthenticationConfigBuilder()
                .addOpenidScope(false)
                .pkceRequired()
                .idTokenRequired(false)
                .scopes("user-read-private", "user-read-email")
                .build();
        return OidcTenantConfig
                .authServerUrl("https://accounts.spotify.com")
                .discoveryEnabled(false)
                .applicationType(WEB_APP)
                .authorizationPath("authorize")
                .tokenPath("api/token")
                .userInfoPath("https://api.spotify.com/v1/me")
                .token().verifyAccessTokenWithUserInfo(true).principalClaim("display_name").end()
                .authentication(authentication)
                .build();
    }

    private static OidcTenantConfig strava() {
        var builder = OidcTenantConfig
                .authServerUrl("https://www.strava.com/oauth")
                .applicationType(WEB_APP)
                .discoveryEnabled(false)
                .authorizationPath("authorize")
                .tokenPath("token")
                .token().verifyAccessTokenWithUserInfo(true).end()
                .userInfoPath("https://www.strava.com/api/v3/athlete");

        builder.authentication()
                .addOpenidScope(false)
                .idTokenRequired(false)
                .scopes("activity:read")
                .redirectPath("/strava")
                .scopeSeparator(",")
                .end();

        builder.credentials().clientSecret().method(QUERY).endCredentials();

        return builder.build();
    }

    private static OidcTenantConfig twitch() {
        // Ref https://dev.twitch.tv/docs/authentication/getting-tokens-oidc/#oidc-authorization-code-grant-flow
        return OidcTenantConfig
                .authServerUrl("https://id.twitch.tv/oauth2")
                .applicationType(WEB_APP)
                .authentication().forceRedirectHttpsScheme().end()
                .credentials().clientSecret().method(POST).endCredentials()
                .build();
    }

    private static OidcTenantConfig discord() {
        // Ref https://discord.com/developers/docs/topics/oauth2
        return OidcTenantConfig
                .authServerUrl("https://discord.com/api/oauth2")
                .applicationType(WEB_APP)
                .discoveryEnabled(false)
                .authorizationPath("authorize")
                .tokenPath("token")
                .jwksPath("keys")
                .token().verifyAccessTokenWithUserInfo(true).end()
                .authentication().scopes("identify", "email").idTokenRequired(false).end()
                .userInfoPath("https://discord.com/api/users/@me")
                .build();
    }
}
