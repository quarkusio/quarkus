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
    public void testLatestLogoutTokenForSubjectIsKept() {
        BackChannelLogoutTokenCache cache = newCache(true, "sub");
        TokenVerificationResult newerLogoutToken = logoutToken("alice", null, 1000);
        cache.addTokenVerification(newerLogoutToken);
        cache.addTokenVerification(logoutToken("alice", null, 990));
        assertSame(newerLogoutToken, cache.findLogoutToken(idToken("alice", "sid-1", 995)));

        TokenVerificationResult newestLogoutToken = logoutToken("alice", null, 1010);
        cache.addTokenVerification(newestLogoutToken);
        assertSame(newestLogoutToken, cache.findLogoutToken(idToken("alice", "sid-1", 1005)));
    }

    @Test
    public void testSessionIdLogoutTokenKey() {
        BackChannelLogoutTokenCache cache = newCache(true, "sid");
        TokenVerificationResult logoutToken = logoutToken("alice", "sid-1", 1000);
        cache.addTokenVerification(logoutToken);

        assertSame(logoutToken, cache.findLogoutToken(idToken("alice", "sid-1", 900)));
        assertNull(cache.findLogoutToken(idToken("bob", "sid-1", 900)));
        assertNull(cache.findLogoutToken(idToken("alice", null, 900)));
        cache.logoutCompleted(logoutToken);
        assertNull(cache.findLogoutToken(idToken("alice", "sid-1", 900)));
    }

    private static BackChannelLogoutTokenCache newCache(boolean logoutAllSessions, String logoutTokenKey) {
        OidcTenantConfig config = OidcTenantConfig.builder().logout().backchannel()
                .logoutAllSessions(logoutAllSessions).logoutTokenKey(logoutTokenKey).endLogout().build();
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
