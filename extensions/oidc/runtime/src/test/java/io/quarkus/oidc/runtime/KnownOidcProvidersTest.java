package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Secret.Method;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.runtime.OidcTenantConfig.Authentication.ResponseMode;
import io.quarkus.oidc.runtime.OidcTenantConfig.Provider;
import io.quarkus.oidc.runtime.providers.KnownOidcProviders;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;

public class KnownOidcProvidersTest {

    @Test
    public void testAcceptGitHubProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.GITHUB));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());
        assertFalse(config.isDiscoveryEnabled().get());
        assertEquals("https://github.com/login/oauth", config.getAuthServerUrl().get());
        assertEquals("authorize", config.getAuthorizationPath().get());
        assertEquals("access_token", config.getTokenPath().get());
        assertEquals("https://api.github.com/user", config.getUserInfoPath().get());

        assertFalse(config.authentication.idTokenRequired.get());
        assertTrue(config.authentication.userInfoRequired.get());
        assertTrue(config.token.verifyAccessTokenWithUserInfo.get());
        assertEquals(List.of("user:email"), config.authentication.scopes.get());
        assertEquals("name", config.getToken().getPrincipalClaim().get());
    }

    @Test
    public void testOverrideGitHubProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .discoveryEnabled(true)
                .authServerUrl("http://localhost/wiremock")
                .authorizationPath("authorization")
                .tokenPath("tokens")
                .userInfoPath("userinfo")
                .authentication().idTokenRequired(true).userInfoRequired(false).scopes("write").end()
                .token().verifyAccessTokenWithUserInfo(false).principalClaim("firstname").end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.GITHUB));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertTrue(config.isDiscoveryEnabled().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals("authorization", config.getAuthorizationPath().get());
        assertEquals("tokens", config.getTokenPath().get());
        assertEquals("userinfo", config.getUserInfoPath().get());

        assertTrue(config.authentication.idTokenRequired.get());
        assertFalse(config.authentication.userInfoRequired.get());
        assertFalse(config.token.verifyAccessTokenWithUserInfo.get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertEquals("firstname", config.getToken().getPrincipalClaim().get());
    }

    @Test
    public void testAcceptTwitterProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.TWITTER));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());
        assertFalse(config.isDiscoveryEnabled().get());
        assertEquals("https://api.twitter.com/2/oauth2", config.getAuthServerUrl().get());
        assertEquals("https://twitter.com/i/oauth2/authorize", config.getAuthorizationPath().get());
        assertEquals("token", config.getTokenPath().get());
        assertEquals("https://api.twitter.com/2/users/me", config.getUserInfoPath().get());

        assertFalse(config.authentication.idTokenRequired.get());
        assertTrue(config.authentication.userInfoRequired.get());
        assertFalse(config.authentication.addOpenidScope.get());
        assertEquals(List.of("offline.access", "tweet.read", "users.read"), config.authentication.scopes.get());
        assertTrue(config.authentication.pkceRequired.get());
    }

    @Test
    public void testOverrideTwitterProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .discoveryEnabled(true)
                .authServerUrl("http://localhost/wiremock")
                .authorizationPath("authorization")
                .tokenPath("tokens")
                .userInfoPath("userinfo")
                .authentication().idTokenRequired(true).userInfoRequired(false).addOpenidScope(true)
                .pkceRequired(false).scopes("write").end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.TWITTER));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertTrue(config.isDiscoveryEnabled().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals("authorization", config.getAuthorizationPath().get());
        assertEquals("tokens", config.getTokenPath().get());
        assertEquals("userinfo", config.getUserInfoPath().get());

        assertTrue(config.authentication.idTokenRequired.get());
        assertFalse(config.authentication.userInfoRequired.get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertTrue(config.authentication.addOpenidScope.get());
        assertFalse(config.authentication.pkceRequired.get());
    }

    @Test
    public void testAcceptMastodonProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.MASTODON));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());
        assertFalse(config.isDiscoveryEnabled().get());
        assertEquals("https://mastodon.social", config.getAuthServerUrl().get());
        assertEquals("/oauth/authorize", config.getAuthorizationPath().get());
        assertEquals("/oauth/token", config.getTokenPath().get());
        assertEquals("/api/v1/accounts/verify_credentials", config.getUserInfoPath().get());

        assertFalse(config.authentication.idTokenRequired.get());
        assertTrue(config.authentication.userInfoRequired.get());
        assertFalse(config.authentication.addOpenidScope.get());
        assertEquals(List.of("read"), config.authentication.scopes.get());
    }

    @Test
    public void testOverrideMastodonProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .discoveryEnabled(true)
                .authServerUrl("http://localhost/wiremock")
                .authorizationPath("authorization")
                .tokenPath("tokens")
                .userInfoPath("userinfo")
                .authentication().idTokenRequired(true).userInfoRequired(false).addOpenidScope(true)
                .scopes("write").end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.MASTODON));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertTrue(config.isDiscoveryEnabled().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals("authorization", config.getAuthorizationPath().get());
        assertEquals("tokens", config.getTokenPath().get());
        assertEquals("userinfo", config.getUserInfoPath().get());

        assertTrue(config.authentication.idTokenRequired.get());
        assertFalse(config.authentication.userInfoRequired.get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertTrue(config.authentication.addOpenidScope.get());
    }

    @Test
    public void testAcceptXProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.X));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());
        assertFalse(config.isDiscoveryEnabled().get());
        assertEquals("https://api.twitter.com/2/oauth2", config.getAuthServerUrl().get());
        assertEquals("https://twitter.com/i/oauth2/authorize", config.getAuthorizationPath().get());
        assertEquals("token", config.getTokenPath().get());
        assertEquals("https://api.twitter.com/2/users/me", config.getUserInfoPath().get());

        assertFalse(config.authentication.idTokenRequired.get());
        assertTrue(config.authentication.userInfoRequired.get());
        assertFalse(config.authentication.addOpenidScope.get());
        assertEquals(List.of("offline.access", "tweet.read", "users.read"), config.authentication.scopes.get());
        assertTrue(config.authentication.pkceRequired.get());
    }

    @Test
    public void testOverrideXProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .discoveryEnabled(true)
                .authServerUrl("http://localhost/wiremock")
                .authorizationPath("authorization")
                .tokenPath("tokens")
                .userInfoPath("userinfo")
                .authentication().idTokenRequired(true).userInfoRequired(false).addOpenidScope(true)
                .pkceRequired(false).scopes("write").end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.X));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertTrue(config.isDiscoveryEnabled().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals("authorization", config.getAuthorizationPath().get());
        assertEquals("tokens", config.getTokenPath().get());
        assertEquals("userinfo", config.getUserInfoPath().get());

        assertTrue(config.authentication.idTokenRequired.get());
        assertFalse(config.authentication.userInfoRequired.get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertTrue(config.authentication.addOpenidScope.get());
        assertFalse(config.authentication.pkceRequired.get());
    }

    @Test
    public void testAcceptFacebookProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.FACEBOOK));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());
        assertFalse(config.isDiscoveryEnabled().get());
        assertEquals("https://www.facebook.com", config.getAuthServerUrl().get());
        assertEquals("https://facebook.com/dialog/oauth/", config.getAuthorizationPath().get());
        assertEquals("https://www.facebook.com/.well-known/oauth/openid/jwks/", config.getJwksPath().get());
        assertEquals("https://graph.facebook.com/v12.0/oauth/access_token", config.getTokenPath().get());

        assertEquals(List.of("email", "public_profile"), config.authentication.scopes.get());
        assertTrue(config.authentication.forceRedirectHttpsScheme.get());
    }

    @Test
    public void testOverrideFacebookProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .discoveryEnabled(true)
                .authServerUrl("http://localhost/wiremock")
                .authorizationPath("authorization")
                .jwksPath("jwks")
                .tokenPath("tokens")
                .authentication().scopes("write").forceRedirectHttpsScheme(false).end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.FACEBOOK));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertTrue(config.isDiscoveryEnabled().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals("authorization", config.getAuthorizationPath().get());
        assertFalse(config.getAuthentication().isForceRedirectHttpsScheme().get());
        assertEquals("jwks", config.getJwksPath().get());
        assertEquals("tokens", config.getTokenPath().get());

        assertEquals(List.of("write"), config.authentication.scopes.get());
    }

    @Test
    public void testAcceptGoogleProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.GOOGLE));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());
        assertEquals("https://accounts.google.com", config.getAuthServerUrl().get());
        assertEquals("name", config.getToken().getPrincipalClaim().get());
        assertEquals(List.of("openid", "email", "profile"), config.authentication.scopes.get());
        assertTrue(config.token.verifyAccessTokenWithUserInfo.get());
    }

    @Test
    public void testOverrideGoogleProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .authServerUrl("http://localhost/wiremock")
                .authentication().scopes("write").end()
                .token().principalClaim("firstname").verifyAccessTokenWithUserInfo(false).end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.GOOGLE));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals("firstname", config.getToken().getPrincipalClaim().get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertFalse(config.token.verifyAccessTokenWithUserInfo.get());
    }

    @Test
    public void testAcceptMicrosoftProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.MICROSOFT));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());
        assertEquals("https://login.microsoftonline.com/common/v2.0", config.getAuthServerUrl().get());
        assertEquals(List.of("openid", "email", "profile"), config.authentication.scopes.get());
        assertEquals("any", config.getToken().getIssuer().get());
    }

    @Test
    public void testOverrideMicrosoftProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .authServerUrl("http://localhost/wiremock")
                .token().issuer("http://localhost/wiremock").end()
                .authentication().scopes("write").forceRedirectHttpsScheme(false).end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.MICROSOFT));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertEquals("http://localhost/wiremock", config.getToken().getIssuer().get());
        assertFalse(config.authentication.forceRedirectHttpsScheme.get());
    }

    @Test
    public void testAcceptAppleProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.APPLE));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());
        assertEquals("https://appleid.apple.com/", config.getAuthServerUrl().get());
        assertEquals(List.of("openid", "email", "name"), config.authentication.scopes.get());
        assertEquals(ResponseMode.FORM_POST, config.authentication().responseMode().get());
        assertEquals(Method.POST_JWT, config.credentials().clientSecret().method().get());
        assertEquals("https://appleid.apple.com/", config.credentials.jwt.audience.get());
        assertEquals(SignatureAlgorithm.ES256.getAlgorithm(), config.credentials.jwt.signatureAlgorithm.get());
        assertTrue(config.authentication.forceRedirectHttpsScheme.get());
    }

    @Test
    public void testOverrideAppleProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .authServerUrl("http://localhost/wiremock")
                .authentication().scopes("write").responseMode(ResponseMode.QUERY).end()
                .credentials().clientSecret().method(Method.POST).end()
                .jwt().audience("http://localhost/audience")
                .signatureAlgorithm(SignatureAlgorithm.ES256.getAlgorithm()).end().end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.APPLE));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertEquals(ResponseMode.QUERY, config.authentication().responseMode().get());
        assertEquals(Method.POST, config.credentials().clientSecret().method().get());
        assertEquals("http://localhost/audience", config.credentials.jwt.audience.get());
        assertEquals(SignatureAlgorithm.ES256.getAlgorithm(), config.credentials.jwt.signatureAlgorithm.get());
    }

    @Test
    public void testAcceptSpotifyProperties() {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.SPOTIFY));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());
        assertEquals("https://accounts.spotify.com", config.getAuthServerUrl().get());
        assertEquals(List.of("user-read-private", "user-read-email"), config.authentication.scopes.get());
        assertTrue(config.token.verifyAccessTokenWithUserInfo.get());
        assertEquals("display_name", config.getToken().getPrincipalClaim().get());
    }

    @Test
    public void testOverrideSpotifyProperties() {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .authServerUrl("http://localhost/wiremock")
                .token().issuer("http://localhost/wiremock").principalClaim("firstname")
                .verifyAccessTokenWithUserInfo(false).end()
                .authentication().scopes("write").forceRedirectHttpsScheme(false).end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.SPOTIFY));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertEquals("http://localhost/wiremock", config.getToken().getIssuer().get());
        assertFalse(config.authentication.forceRedirectHttpsScheme.get());
        assertEquals("firstname", config.getToken().getPrincipalClaim().get());
        assertFalse(config.token.verifyAccessTokenWithUserInfo.get());
    }

    @Test
    public void testAcceptStravaProperties() {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.STRAVA));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());

        assertFalse(config.discoveryEnabled.get());
        assertEquals("https://www.strava.com/oauth", config.getAuthServerUrl().get());
        assertEquals("authorize", config.getAuthorizationPath().get());
        assertEquals("token", config.getTokenPath().get());
        assertEquals("https://www.strava.com/api/v3/athlete", config.getUserInfoPath().get());
        assertEquals(List.of("activity:read"), config.authentication.scopes.get());
        assertTrue(config.token.verifyAccessTokenWithUserInfo.get());
        assertFalse(config.getAuthentication().idTokenRequired.get());
        assertEquals(Method.QUERY, config.credentials().clientSecret().method().get());
        assertEquals("/strava", config.authentication.redirectPath.get());
        assertEquals(",", config.authentication.scopeSeparator.get());
    }

    @Test
    public void testOverrideStravaProperties() {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .authServerUrl("http://localhost/wiremock")
                .authorizationPath("authorizations")
                .tokenPath("tokens")
                .userInfoPath("users")
                .authentication().scopes("write").redirectPath("/fitness-app").scopeSeparator(" ").end()
                .token().verifyAccessTokenWithUserInfo(false).end()
                .credentials().clientSecret().method(Method.BASIC).end().end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.STRAVA));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals("authorizations", config.getAuthorizationPath().get());
        assertEquals("tokens", config.getTokenPath().get());
        assertEquals("users", config.getUserInfoPath().get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertFalse(config.token.verifyAccessTokenWithUserInfo.get());
        assertEquals(Method.BASIC, config.credentials().clientSecret().method().get());
        assertEquals("/fitness-app", config.authentication.redirectPath.get());
        assertEquals(" ", config.authentication.scopeSeparator.get());
    }

    @Test
    public void testAcceptTwitchProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.TWITCH));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());
        assertEquals("https://id.twitch.tv/oauth2", config.getAuthServerUrl().get());
        assertEquals(Method.POST, config.credentials().clientSecret().method().get());
        assertTrue(config.authentication.forceRedirectHttpsScheme.get());
    }

    @Test
    public void testOverrideTwitchProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .authServerUrl("http://localhost/wiremock")
                .credentials().clientSecret().method(Method.BASIC).end().end()
                .authentication().forceRedirectHttpsScheme(false).end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.FACEBOOK));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertFalse(config.getAuthentication().isForceRedirectHttpsScheme().get());
        assertEquals(Method.BASIC, config.credentials().clientSecret().method().get());
    }

    @Test
    public void testAcceptDiscordProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.DISCORD));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());
        assertFalse(config.discoveryEnabled.get());
        assertEquals("https://discord.com/api/oauth2", config.getAuthServerUrl().get());
        assertEquals("authorize", config.getAuthorizationPath().get());
        assertEquals("token", config.getTokenPath().get());
        assertEquals("keys", config.getJwksPath().get());
        assertEquals("https://discord.com/api/users/@me", config.getUserInfoPath().get());
        assertEquals(List.of("identify", "email"), config.authentication.scopes.get());
        assertFalse(config.getAuthentication().idTokenRequired.get());
    }

    @Test
    public void testOverrideDiscordProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .authServerUrl("http://localhost/wiremock")
                .credentials().clientSecret().method(Method.BASIC).end().end()
                .authentication().forceRedirectHttpsScheme(false).end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.DISCORD));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertFalse(config.getAuthentication().isForceRedirectHttpsScheme().get());
        assertEquals(Method.BASIC, config.credentials().clientSecret().method().get());
    }

    @Test
    public void testAcceptLinkedInProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.LINKEDIN));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals("https://www.linkedin.com/oauth", config.getAuthServerUrl().get());
        assertEquals(List.of("email", "profile"), config.authentication.scopes.get());
    }

    @Test
    public void testOverrideLinkedInProperties() throws Exception {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .applicationType(ApplicationType.HYBRID)
                .authServerUrl("http://localhost/wiremock")
                .credentials().clientSecret().method(Method.BASIC).end().end()
                .authentication().forceRedirectHttpsScheme(false).end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.LINKEDIN));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.applicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertFalse(config.getAuthentication().isForceRedirectHttpsScheme().get());
        assertEquals(Method.BASIC, config.credentials().clientSecret().method().get());
    }

    @Test
    public void testAcceptSlackProperties() {
        OidcTenantConfig tenant = OidcTenantConfig.builder().build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.SLACK));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.applicationType().get());
        assertTrue(config.isDiscoveryEnabled().orElse(true));
        assertEquals("https://slack.com", config.getAuthServerUrl().get());

        assertEquals("name", config.token.principalClaim.get());
        assertTrue(config.authentication.forceRedirectHttpsScheme.orElse(false));
        assertEquals(List.of("profile", "email"), config.authentication.scopes.get());
    }

    @Test
    public void testOverrideSlackProperties() {
        OidcTenantConfig tenant = OidcTenantConfig.builder()
                .tenantId("PattiSmith")
                .applicationType(ApplicationType.SERVICE)
                .discoveryEnabled(false)
                .authServerUrl("https://private-slack.com")
                .token().principalClaim("I you my own principal").end()
                .authentication().forceRedirectHttpsScheme(false).scopes("profile").end()
                .build();
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.SLACK));

        assertEquals("PattiSmith", config.getTenantId().get());
        assertEquals(ApplicationType.SERVICE, config.applicationType().get());
        assertFalse(config.isDiscoveryEnabled().orElse(true));
        assertEquals("https://private-slack.com", config.getAuthServerUrl().get());

        assertEquals("I you my own principal", config.token.principalClaim.get());
        assertFalse(config.authentication.forceRedirectHttpsScheme.orElse(false));
        assertEquals(List.of("profile"), config.authentication.scopes.get());
    }
}
