package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Permission;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Authentication.ResponseMode;
import io.quarkus.oidc.OidcTenantConfig.Provider;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials.Secret.Method;
import io.quarkus.oidc.runtime.providers.KnownOidcProviders;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.json.JsonObject;

public class OidcUtilsTest {

    @Test
    public void testAcceptGitHubProperties() throws Exception {
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.GITHUB));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.getApplicationType().get());
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
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);

        tenant.setApplicationType(ApplicationType.HYBRID);
        tenant.setDiscoveryEnabled(true);
        tenant.setAuthServerUrl("http://localhost/wiremock");
        tenant.setAuthorizationPath("authorization");
        tenant.setTokenPath("tokens");
        tenant.setUserInfoPath("userinfo");

        tenant.authentication.setIdTokenRequired(true);
        tenant.authentication.setUserInfoRequired(false);
        tenant.token.setVerifyAccessTokenWithUserInfo(false);
        tenant.authentication.setScopes(List.of("write"));
        tenant.token.setPrincipalClaim("firstname");

        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.GITHUB));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.getApplicationType().get());
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
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.TWITTER));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.getApplicationType().get());
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
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);

        tenant.setApplicationType(ApplicationType.HYBRID);
        tenant.setDiscoveryEnabled(true);
        tenant.setAuthServerUrl("http://localhost/wiremock");
        tenant.setAuthorizationPath("authorization");
        tenant.setTokenPath("tokens");
        tenant.setUserInfoPath("userinfo");

        tenant.authentication.setIdTokenRequired(true);
        tenant.authentication.setUserInfoRequired(false);
        tenant.authentication.setAddOpenidScope(true);
        tenant.authentication.setPkceRequired(false);
        tenant.authentication.setScopes(List.of("write"));

        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.TWITTER));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.getApplicationType().get());
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
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.MASTODON));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.getApplicationType().get());
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
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);

        tenant.setApplicationType(ApplicationType.HYBRID);
        tenant.setDiscoveryEnabled(true);
        tenant.setAuthServerUrl("http://localhost/wiremock");
        tenant.setAuthorizationPath("authorization");
        tenant.setTokenPath("tokens");
        tenant.setUserInfoPath("userinfo");

        tenant.authentication.setIdTokenRequired(true);
        tenant.authentication.setUserInfoRequired(false);
        tenant.authentication.setAddOpenidScope(true);
        tenant.authentication.setScopes(List.of("write"));

        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.MASTODON));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.getApplicationType().get());
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
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.X));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.getApplicationType().get());
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
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);

        tenant.setApplicationType(ApplicationType.HYBRID);
        tenant.setDiscoveryEnabled(true);
        tenant.setAuthServerUrl("http://localhost/wiremock");
        tenant.setAuthorizationPath("authorization");
        tenant.setTokenPath("tokens");
        tenant.setUserInfoPath("userinfo");

        tenant.authentication.setIdTokenRequired(true);
        tenant.authentication.setUserInfoRequired(false);
        tenant.authentication.setAddOpenidScope(true);
        tenant.authentication.setPkceRequired(false);
        tenant.authentication.setScopes(List.of("write"));

        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.X));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.getApplicationType().get());
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
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.FACEBOOK));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.getApplicationType().get());
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
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);

        tenant.setApplicationType(ApplicationType.HYBRID);
        tenant.setDiscoveryEnabled(true);
        tenant.setAuthServerUrl("http://localhost/wiremock");
        tenant.setAuthorizationPath("authorization");
        tenant.setJwksPath("jwks");
        tenant.setTokenPath("tokens");

        tenant.authentication.setScopes(List.of("write"));
        tenant.authentication.setForceRedirectHttpsScheme(false);

        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.FACEBOOK));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.getApplicationType().get());
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
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.GOOGLE));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.getApplicationType().get());
        assertEquals("https://accounts.google.com", config.getAuthServerUrl().get());
        assertEquals("name", config.getToken().getPrincipalClaim().get());
        assertEquals(List.of("openid", "email", "profile"), config.authentication.scopes.get());
        assertTrue(config.token.verifyAccessTokenWithUserInfo.get());
    }

    @Test
    public void testOverrideGoogleProperties() throws Exception {
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);

        tenant.setApplicationType(ApplicationType.HYBRID);
        tenant.setAuthServerUrl("http://localhost/wiremock");
        tenant.authentication.setScopes(List.of("write"));
        tenant.token.setPrincipalClaim("firstname");
        tenant.token.setVerifyAccessTokenWithUserInfo(false);

        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.GOOGLE));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.getApplicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals("firstname", config.getToken().getPrincipalClaim().get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertFalse(config.token.verifyAccessTokenWithUserInfo.get());
    }

    @Test
    public void testAcceptMicrosoftProperties() throws Exception {
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.MICROSOFT));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.getApplicationType().get());
        assertEquals("https://login.microsoftonline.com/common/v2.0", config.getAuthServerUrl().get());
        assertEquals(List.of("openid", "email", "profile"), config.authentication.scopes.get());
        assertEquals("any", config.getToken().getIssuer().get());
    }

    @Test
    public void testOverrideMicrosoftProperties() throws Exception {
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);

        tenant.setApplicationType(ApplicationType.HYBRID);
        tenant.setAuthServerUrl("http://localhost/wiremock");
        tenant.getToken().setIssuer("http://localhost/wiremock");
        tenant.authentication.setScopes(List.of("write"));
        tenant.authentication.setForceRedirectHttpsScheme(false);

        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.MICROSOFT));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.getApplicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertEquals("http://localhost/wiremock", config.getToken().getIssuer().get());
        assertFalse(config.authentication.forceRedirectHttpsScheme.get());
    }

    @Test
    public void testAcceptAppleProperties() throws Exception {
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.APPLE));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.getApplicationType().get());
        assertEquals("https://appleid.apple.com/", config.getAuthServerUrl().get());
        assertEquals(List.of("openid", "email", "name"), config.authentication.scopes.get());
        assertEquals(ResponseMode.FORM_POST, config.authentication.responseMode.get());
        assertEquals(Method.POST_JWT, config.credentials.clientSecret.method.get());
        assertEquals("https://appleid.apple.com/", config.credentials.jwt.audience.get());
        assertEquals(SignatureAlgorithm.ES256.getAlgorithm(), config.credentials.jwt.signatureAlgorithm.get());
        assertTrue(config.authentication.forceRedirectHttpsScheme.get());
    }

    @Test
    public void testOverrideAppleProperties() throws Exception {
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);

        tenant.setApplicationType(ApplicationType.HYBRID);
        tenant.setAuthServerUrl("http://localhost/wiremock");
        tenant.authentication.setScopes(List.of("write"));
        tenant.authentication.setResponseMode(ResponseMode.QUERY);
        tenant.credentials.clientSecret.setMethod(Method.POST);
        tenant.credentials.jwt.setAudience("http://localhost/audience");
        tenant.credentials.jwt.setSignatureAlgorithm(SignatureAlgorithm.ES256.getAlgorithm());

        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.APPLE));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.getApplicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertEquals(ResponseMode.QUERY, config.authentication.responseMode.get());
        assertEquals(Method.POST, config.credentials.clientSecret.method.get());
        assertEquals("http://localhost/audience", config.credentials.jwt.audience.get());
        assertEquals(SignatureAlgorithm.ES256.getAlgorithm(), config.credentials.jwt.signatureAlgorithm.get());
    }

    @Test
    public void testAcceptSpotifyProperties() {
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.SPOTIFY));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.getApplicationType().get());
        assertEquals("https://accounts.spotify.com", config.getAuthServerUrl().get());
        assertEquals(List.of("user-read-private", "user-read-email"), config.authentication.scopes.get());
        assertTrue(config.token.verifyAccessTokenWithUserInfo.get());
        assertEquals("display_name", config.getToken().getPrincipalClaim().get());
    }

    @Test
    public void testOverrideSpotifyProperties() {
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);

        tenant.setApplicationType(ApplicationType.HYBRID);
        tenant.setAuthServerUrl("http://localhost/wiremock");
        tenant.getToken().setIssuer("http://localhost/wiremock");
        tenant.authentication.setScopes(List.of("write"));
        tenant.authentication.setForceRedirectHttpsScheme(false);
        tenant.token.setPrincipalClaim("firstname");
        tenant.token.setVerifyAccessTokenWithUserInfo(false);

        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.SPOTIFY));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.getApplicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertEquals(List.of("write"), config.authentication.scopes.get());
        assertEquals("http://localhost/wiremock", config.getToken().getIssuer().get());
        assertFalse(config.authentication.forceRedirectHttpsScheme.get());
        assertEquals("firstname", config.getToken().getPrincipalClaim().get());
        assertFalse(config.token.verifyAccessTokenWithUserInfo.get());
    }

    @Test
    public void testAcceptTwitchProperties() throws Exception {
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);
        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.TWITCH));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.WEB_APP, config.getApplicationType().get());
        assertEquals("https://id.twitch.tv/oauth2", config.getAuthServerUrl().get());
        assertEquals(Method.POST, config.credentials.clientSecret.method.get());
        assertTrue(config.authentication.forceRedirectHttpsScheme.get());
    }

    @Test
    public void testOverrideTwitchProperties() throws Exception {
        OidcTenantConfig tenant = new OidcTenantConfig();
        tenant.setTenantId(OidcUtils.DEFAULT_TENANT_ID);

        tenant.setApplicationType(ApplicationType.HYBRID);
        tenant.setAuthServerUrl("http://localhost/wiremock");
        tenant.credentials.clientSecret.setMethod(Method.BASIC);
        tenant.authentication.setForceRedirectHttpsScheme(false);

        OidcTenantConfig config = OidcUtils.mergeTenantConfig(tenant, KnownOidcProviders.provider(Provider.FACEBOOK));

        assertEquals(OidcUtils.DEFAULT_TENANT_ID, config.getTenantId().get());
        assertEquals(ApplicationType.HYBRID, config.getApplicationType().get());
        assertEquals("http://localhost/wiremock", config.getAuthServerUrl().get());
        assertFalse(config.getAuthentication().isForceRedirectHttpsScheme().get());
        assertEquals(Method.BASIC, config.credentials.clientSecret.method.get());
    }

    @Test
    public void testCorrectTokenType() throws Exception {
        OidcTenantConfig.Token tokenClaims = new OidcTenantConfig.Token();
        tokenClaims.setTokenType("access_token");
        JsonObject json = new JsonObject();
        json.put("typ", "access_token");
        OidcUtils.validatePrimaryJwtTokenType(tokenClaims, json);
    }

    @Test
    public void testWrongTokenType() throws Exception {
        OidcTenantConfig.Token tokenClaims = new OidcTenantConfig.Token();
        tokenClaims.setTokenType("access_token");
        JsonObject json = new JsonObject();
        json.put("typ", "refresh_token");
        try {
            OidcUtils.validatePrimaryJwtTokenType(tokenClaims, json);
            fail("Exception expected: wrong token type");
        } catch (OIDCException ex) {
            // expected
        }
    }

    @Test
    public void testKeycloakRefreshTokenType() throws Exception {
        JsonObject json = new JsonObject();
        json.put("typ", "Refresh");
        try {
            OidcUtils.validatePrimaryJwtTokenType(new OidcTenantConfig.Token(), json);
            fail("Exception expected: wrong token type");
        } catch (OIDCException ex) {
            // expected
        }
    }

    @Test
    public void testKeycloakRealmAccessToken() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath(null);
        List<String> roles = OidcUtils.findRoles(null, rolesCfg,
                read(getClass().getResourceAsStream("/tokenKeycloakRealmAccess.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("role1"));
        assertTrue(roles.contains("role2"));
    }

    @Test
    public void testKeycloakRealmAndResourceAccessTokenClient1() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath(null);
        List<String> roles = OidcUtils.findRoles("client1", rolesCfg,
                read(getClass().getResourceAsStream("/tokenKeycloakResourceAccess.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("realm1"));
        assertTrue(roles.contains("role1"));
    }

    @Test
    public void testKeycloakRealmAndResourceAccessTokenClient2() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath(null);
        List<String> roles = OidcUtils.findRoles("client2", rolesCfg,
                read(getClass().getResourceAsStream("/tokenKeycloakResourceAccess.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("realm1"));
        assertTrue(roles.contains("role2"));
    }

    @Test
    public void testKeycloakRealmAndResourceAccessTokenNullClient() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath(null);
        List<String> roles = OidcUtils.findRoles(null, rolesCfg,
                read(getClass().getResourceAsStream("/tokenKeycloakResourceAccess.json")));
        assertEquals(1, roles.size());
        assertTrue(roles.contains("realm1"));
    }

    @Test
    public void testTokenWithGroups() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath(null);
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenGroups.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("group1"));
        assertTrue(roles.contains("group2"));
    }

    @Test
    public void testTokenWithCustomRoles() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPath(Collections.singletonList("application_card/embedded/roles"));
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenCustomPath.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("r1"));
        assertTrue(roles.contains("r2"));
    }

    @Test
    public void testTokenWithMultipleCustomRolePaths() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPath(List.of("application_card/embedded/roles", "application_card/embedded2/roles"));
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenCustomPath.json")));
        assertEquals(4, roles.size());
        assertTrue(roles.contains("r1"));
        assertTrue(roles.contains("r2"));
        assertTrue(roles.contains("r5"));
        assertTrue(roles.contains("r6"));
    }

    @Test
    public void testTokenWithCustomNamespacedRoles() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPath(Collections.singletonList("application_card/embedded/\"https://custom/roles\""));
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenCustomPath.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("r3"));
        assertTrue(roles.contains("r4"));
    }

    @Test
    public void testTokenWithCustomNamespacedRolesWithSpaces() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPath(Collections.singletonList(" application_card/embedded/\"https://custom/roles\" "));
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenCustomPath.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("r3"));
        assertTrue(roles.contains("r4"));
    }

    @Test
    public void testTokenWithScope() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles.fromClaimPath(Collections.singletonList("scope"));
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(getClass().getResourceAsStream("/tokenScope.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("s1"));
        assertTrue(roles.contains("s2"));
    }

    @Test
    public void testTokenWithCustomScope() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPathAndSeparator(Collections.singletonList("customScope"), ",");
        List<String> roles = OidcUtils.findRoles(null, rolesCfg,
                read(getClass().getResourceAsStream("/tokenCustomScope.json")));
        assertEquals(2, roles.size());
        assertTrue(roles.contains("s1"));
        assertTrue(roles.contains("s2"));
    }

    @Test
    public void testTokenWithCustomRolesWrongPath() throws Exception {
        OidcTenantConfig.Roles rolesCfg = OidcTenantConfig.Roles
                .fromClaimPath(Collections.singletonList("application-card/embedded/roles"));
        InputStream is = getClass().getResourceAsStream("/tokenCustomPath.json");
        List<String> roles = OidcUtils.findRoles(null, rolesCfg, read(is));
        assertEquals(0, roles.size());
    }

    @Test
    public void testTokenIsOpaque() throws Exception {
        assertTrue(OidcUtils.isOpaqueToken("123"));
        assertTrue(OidcUtils.isOpaqueToken("1.23"));
        assertFalse(OidcUtils.isOpaqueToken("1.2.3"));
    }

    @Test
    public void testDecodeOpaqueTokenAsJwt() throws Exception {
        assertNull(OidcUtils.decodeJwtContent("123"));
        assertNull(OidcUtils.decodeJwtContent("1.23"));
        assertNull(OidcUtils.decodeJwtContent("1.2.3"));
    }

    @Test
    public void testDecodeJwt() throws Exception {
        final byte[] keyBytes = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
                .getBytes(StandardCharsets.UTF_8);
        SecretKey key = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HMACSHA256");
        String jwt = Jwt.claims().sign(key);
        assertNull(OidcUtils.decodeJwtContent(jwt + ".4"));
        JsonObject json = OidcUtils.decodeJwtContent(jwt);
        assertTrue(json.containsKey("iat"));
        assertTrue(json.containsKey("exp"));
        assertTrue(json.containsKey("jti"));
    }

    @Test
    public void testTransformScopeToPermission() throws Exception {
        Permission[] perms = OidcUtils.transformScopesToPermissions(
                List.of("read", "read:d", "read:", ":read"));
        assertEquals(4, perms.length);

        assertEquals("read", perms[0].getName());
        assertNull(perms[0].getActions());
        assertEquals("read", perms[1].getName());
        assertEquals("d", perms[1].getActions());
        assertEquals("read:", perms[2].getName());
        assertNull(perms[2].getActions());
        assertEquals(":read", perms[3].getName());
        assertNull(perms[3].getActions());
    }

    public static JsonObject read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return new JsonObject(buffer.lines().collect(Collectors.joining("\n")));
        }
    }

}
