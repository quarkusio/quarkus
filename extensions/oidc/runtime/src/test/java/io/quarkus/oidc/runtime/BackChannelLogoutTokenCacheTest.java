package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.vertx.core.json.JsonObject;

public class BackChannelLogoutTokenCacheTest {

    @Test
    public void testLogoutTokensWithSessionIdAreCachedPerSession() {
        BackChannelLogoutTokenCache cache = newCache(false, "sub");
        TokenVerificationResult session1LogoutToken = logoutToken("alice", "sid-1", 1000);
        TokenVerificationResult session2LogoutToken = logoutToken("alice", "sid-2", 1000);
        cache.addTokenVerification(session1LogoutToken);
        cache.addTokenVerification(session2LogoutToken);

        assertSame(session1LogoutToken, cache.findLogoutToken(idToken("alice", "sid-1", 900)));
        assertSame(session2LogoutToken, cache.findLogoutToken(idToken("alice", "sid-2", 900)));
        assertNull(cache.findLogoutToken(idToken("alice", "sid-3", 900)));
        assertNull(cache.findLogoutToken(idToken("bob", "sid-1", 900)));
        assertNull(cache.findLogoutToken(idToken("alice", "sid-1", 900).put("iss", "https://other-issuer.example.com")));

        cache.logoutCompleted(session1LogoutToken);
        assertNull(cache.findLogoutToken(idToken("alice", "sid-1", 900)));
        assertSame(session2LogoutToken, cache.findLogoutToken(idToken("alice", "sid-2", 900)));
    }

    @Test
    public void testLogoutTokenWithoutSessionIdLogsOutFirstSessionByDefault() {
        BackChannelLogoutTokenCache cache = newCache(false, "sub");
        TokenVerificationResult logoutToken = logoutToken("alice", null, 1000);
        cache.addTokenVerification(logoutToken);

        assertSame(logoutToken, cache.findLogoutToken(idToken("alice", "sid-1", 900)));
        cache.logoutCompleted(logoutToken);
        assertNull(cache.findLogoutToken(idToken("alice", "sid-2", 900)));
    }

    @Test
    public void testLogoutTokenWithoutSessionIdLogsOutAllSessions() {
        BackChannelLogoutTokenCache cache = newCache(true, "sub");
        TokenVerificationResult logoutToken = logoutToken("alice", null, 1000);
        cache.addTokenVerification(logoutToken);

        assertSame(logoutToken, cache.findLogoutToken(idToken("alice", "sid-1", 900)));
        cache.logoutCompleted(logoutToken);
        assertSame(logoutToken, cache.findLogoutToken(idToken("alice", "sid-2", 1000)));
        assertSame(logoutToken, cache.findLogoutToken(idToken("alice", null, 900)));
        assertNull(cache.findLogoutToken(idToken("bob", "sid-1", 900)));
        // session created after the logout token was issued
        assertNull(cache.findLogoutToken(idToken("alice", "sid-3", 1001)));

        // logout token with the session id is dropped once used and targets its session whenever it was created
        TokenVerificationResult sessionLogoutToken = logoutToken("alice", "sid-3", 1000);
        cache.addTokenVerification(sessionLogoutToken);
        assertSame(sessionLogoutToken, cache.findLogoutToken(idToken("alice", "sid-3", 1001)));
        cache.logoutCompleted(sessionLogoutToken);
        assertNull(cache.findLogoutToken(idToken("alice", "sid-3", 1001)));
        assertSame(logoutToken, cache.findLogoutToken(idToken("alice", "sid-2", 1000)));
    }

    @Test
    public void testLogoutTokenWithoutSessionIdRemovesEarlierSessionLogoutTokens() {
        BackChannelLogoutTokenCache cache = newCache(true, "sub");
        TokenVerificationResult session1LogoutToken = logoutToken("alice", "sid-1", 1000);
        TokenVerificationResult session2LogoutToken = logoutToken("alice", "sid-2", 1005);
        TokenVerificationResult bobLogoutToken = logoutToken("bob", "sid-3", 1000);
        // issued after the logout token without the session id, it may target a session created after it
        TokenVerificationResult laterSessionLogoutToken = logoutToken("alice", "sid-4", 1010);
        cache.addTokenVerification(session1LogoutToken);
        cache.addTokenVerification(session2LogoutToken);
        cache.addTokenVerification(bobLogoutToken);
        cache.addTokenVerification(laterSessionLogoutToken);

        TokenVerificationResult logoutToken = logoutToken("alice", null, 1005);
        cache.addTokenVerification(logoutToken);

        // sessions targeted by the earlier logout tokens are logged out by the logout token without the session id
        assertSame(logoutToken, cache.findLogoutToken(idToken("alice", "sid-1", 900)));
        assertSame(logoutToken, cache.findLogoutToken(idToken("alice", "sid-2", 1000)));
        assertSame(bobLogoutToken, cache.findLogoutToken(idToken("bob", "sid-3", 900)));
        assertSame(laterSessionLogoutToken, cache.findLogoutToken(idToken("alice", "sid-4", 1008)));
        assertNull(cache.findLogoutToken(idToken("alice", "sid-5", 1008)));
    }

    @Test
    public void testLogoutTokenWithoutSessionIdDoesNotEvictOtherTokensFromFullCache() {
        BackChannelLogoutTokenCache cache = newCache(true, "sub", 3);
        TokenVerificationResult bobLogoutToken = logoutToken("bob", null, 1000);
        cache.addTokenVerification(bobLogoutToken);
        cache.addTokenVerification(logoutToken("alice", "sid-1", 1000));
        cache.addTokenVerification(logoutToken("alice", "sid-2", 1000));

        // the redundant logout tokens are removed before the new one is cached, so the eldest token is not evicted
        TokenVerificationResult logoutToken = logoutToken("alice", null, 1005);
        cache.addTokenVerification(logoutToken);
        assertSame(bobLogoutToken, cache.findLogoutToken(idToken("bob", "sid-3", 900)));
        assertSame(logoutToken, cache.findLogoutToken(idToken("alice", "sid-1", 900)));
        assertSame(logoutToken, cache.findLogoutToken(idToken("alice", "sid-2", 900)));
    }

    @Test
    public void testLogoutTokenWithoutSessionIdKeepsSessionLogoutTokensByDefault() {
        BackChannelLogoutTokenCache cache = newCache(false, "sub");
        TokenVerificationResult session1LogoutToken = logoutToken("alice", "sid-1", 1000);
        TokenVerificationResult session2LogoutToken = logoutToken("alice", "sid-2", 1000);
        cache.addTokenVerification(session1LogoutToken);
        cache.addTokenVerification(session2LogoutToken);
        TokenVerificationResult logoutToken = logoutToken("alice", null, 1005);
        cache.addTokenVerification(logoutToken);

        // the logout token without the session id logs out a single session by default, it does not cover the targeted ones
        assertSame(session1LogoutToken, cache.findLogoutToken(idToken("alice", "sid-1", 900)));
        cache.logoutCompleted(session1LogoutToken);
        assertSame(session2LogoutToken, cache.findLogoutToken(idToken("alice", "sid-2", 900)));
        cache.logoutCompleted(session2LogoutToken);
        assertSame(logoutToken, cache.findLogoutToken(idToken("alice", "sid-3", 900)));
        cache.logoutCompleted(logoutToken);
        assertNull(cache.findLogoutToken(idToken("alice", "sid-4", 900)));
    }

    @Test
    public void testSessionIdLogoutTokenKey() {
        BackChannelLogoutTokenCache cache = newCache(false, "sid");
        TokenVerificationResult logoutToken = logoutToken("alice", "sid-1", 1000);
        cache.addTokenVerification(logoutToken);

        assertSame(logoutToken, cache.findLogoutToken(idToken("alice", "sid-1", 900)));
        assertNull(cache.findLogoutToken(idToken("bob", "sid-1", 900)));
        assertNull(cache.findLogoutToken(idToken("alice", null, 900)));
        cache.logoutCompleted(logoutToken);
        assertNull(cache.findLogoutToken(idToken("alice", "sid-1", 900)));
    }

    @Test
    public void testCacheKeyCollisionDoesNotMatchAnotherSession() {
        BackChannelLogoutTokenCache cache = newCache(false, "sub");
        // cached with the 'alice|sid-1|sid-2' key which is also the key of the 'alice' subject with the 'sid-1|sid-2' session
        TokenVerificationResult logoutToken = logoutToken("alice|sid-1", "sid-2", 1000);
        cache.addTokenVerification(logoutToken);

        assertNull(cache.findLogoutToken(idToken("alice", "sid-1|sid-2", 900)));
        assertSame(logoutToken, cache.findLogoutToken(idToken("alice|sid-1", "sid-2", 900)));
    }

    private static BackChannelLogoutTokenCache newCache(boolean logoutAllSessions, String logoutTokenKey) {
        OidcTenantConfig config = OidcTenantConfig.builder().logout().backchannel()
                .logoutAllSessions(logoutAllSessions).logoutTokenKey(logoutTokenKey).endLogout().build();
        return new BackChannelLogoutTokenCache(config, null);
    }

    private static BackChannelLogoutTokenCache newCache(boolean logoutAllSessions, String logoutTokenKey, int cacheSize) {
        OidcTenantConfig config = OidcTenantConfig.builder().logout().backchannel().logoutAllSessions(logoutAllSessions)
                .logoutTokenKey(logoutTokenKey).tokenCacheSize(cacheSize).endLogout().build();
        return new BackChannelLogoutTokenCache(config, null);
    }

    private static TokenVerificationResult logoutToken(String sub, String sid, long issuedAt) {
        return new TokenVerificationResult(claims(sub, sid, issuedAt), null);
    }

    private static JsonObject idToken(String sub, String sid, long issuedAt) {
        return claims(sub, sid, issuedAt);
    }

    private static JsonObject claims(String sub, String sid, long issuedAt) {
        JsonObject claims = new JsonObject().put("iss", "https://issuer.example.com").put("sub", sub).put("iat", issuedAt);
        if (sid != null) {
            claims.put("sid", sid);
        }
        return claims;
    }
}
