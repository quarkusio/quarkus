package io.quarkus.oidc.runtime;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

import io.quarkus.oidc.common.runtime.OidcConstants;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class BackChannelLogoutTokenCache {

    private static final Logger LOG = Logger.getLogger(BackChannelLogoutTokenCache.class);
    private static final String CACHE_KEY_SEPARATOR = "|";

    private final MemoryCache<TokenVerificationResult> cache;
    private final String logoutTokenKey;
    private final boolean logoutTokenKeyIsSid;
    private final boolean logoutAllSessions;

    public BackChannelLogoutTokenCache(OidcTenantConfig oidcTenantConfig, Vertx vertx) {
        OidcTenantConfig.Backchannel backchannel = oidcTenantConfig.logout().backchannel();
        cache = new MemoryCache<>(vertx, backchannel.cleanUpTimerInterval(),
                backchannel.tokenCacheTimeToLive(), backchannel.tokenCacheSize());
        logoutTokenKey = backchannel.logoutTokenKey();
        logoutTokenKeyIsSid = OidcConstants.BACK_CHANNEL_LOGOUT_SID_CLAIM.equals(logoutTokenKey);
        logoutAllSessions = backchannel.logoutAllSessions();
    }

    public void addTokenVerification(TokenVerificationResult logoutToken) {
        JsonObject claims = logoutToken.localVerificationResult();
        if (logoutAllSessions && claims.getString(OidcConstants.BACK_CHANNEL_LOGOUT_SID_CLAIM) == null) {
            removeRedundantSessionLogoutTokens(claims);
        }
        cache.add(cacheKey(claims), logoutToken);
    }

    public TokenVerificationResult findLogoutToken(JsonObject idTokenJson) {
        String logoutTokenKeyValue = idTokenJson.getString(logoutTokenKey);
        if (logoutTokenKeyValue == null) {
            return null;
        }
        TokenVerificationResult logoutToken = null;
        if (!logoutTokenKeyIsSid) {
            String idTokenSid = idTokenJson.getString(OidcConstants.ID_TOKEN_SID_CLAIM);
            if (idTokenSid != null) {
                // logout token with the session id targets only the session identified by it
                logoutToken = findLogoutToken(sessionCacheKey(logoutTokenKeyValue, idTokenSid), idTokenJson);
            }
        }
        if (logoutToken == null) {
            // logout token without the session id matches any session of the subject created before the token was issued,
            // logoutCompleted decides whether it is dropped after the first logout or kept to log out all the sessions
            logoutToken = findLogoutToken(logoutTokenKeyValue, idTokenJson);
        }
        return logoutToken;
    }

    public void logoutCompleted(TokenVerificationResult logoutToken) {
        JsonObject claims = logoutToken.localVerificationResult();
        if (logoutAllSessions && claims.getString(OidcConstants.BACK_CHANNEL_LOGOUT_SID_CLAIM) == null) {
            // keep the logout token cached so that the other sessions of the subject are logged out too
            return;
        }
        cache.remove(cacheKey(claims));
    }

    void shutdown(Vertx vertx) {
        cache.stopTimer(vertx);
    }

    private TokenVerificationResult findLogoutToken(String cacheKey, JsonObject idTokenJson) {
        TokenVerificationResult logoutToken = cache.get(cacheKey);
        if (logoutToken != null && targetsSession(logoutToken.localVerificationResult(), idTokenJson)) {
            return logoutToken;
        }
        return null;
    }

    private void removeRedundantSessionLogoutTokens(JsonObject subjectLogoutTokenJson) {
        String logoutTokenKeyValue = subjectLogoutTokenJson.getString(logoutTokenKey);
        Long subjectLogoutTokenIssuedAt = subjectLogoutTokenJson.getLong(Claims.iat.name());
        cache.removeIf(cachedLogoutToken -> {
            JsonObject cachedLogoutTokenJson = cachedLogoutToken.localVerificationResult();
            return cachedLogoutTokenJson.getString(OidcConstants.BACK_CHANNEL_LOGOUT_SID_CLAIM) != null
                    && logoutTokenKeyValue.equals(cachedLogoutTokenJson.getString(logoutTokenKey))
                    && !isIssuedAfter(cachedLogoutTokenJson, subjectLogoutTokenIssuedAt);
        });
    }

    private String cacheKey(JsonObject logoutTokenJson) {
        String logoutTokenKeyValue = logoutTokenJson.getString(logoutTokenKey);
        if (logoutTokenKeyIsSid) {
            return logoutTokenKeyValue;
        }
        String logoutTokenSid = logoutTokenJson.getString(OidcConstants.BACK_CHANNEL_LOGOUT_SID_CLAIM);
        return logoutTokenSid == null ? logoutTokenKeyValue : sessionCacheKey(logoutTokenKeyValue, logoutTokenSid);
    }

    private static String sessionCacheKey(String logoutTokenKeyValue, String sid) {
        // the key is only an index, a collision is harmless because targetsSession
        // compares the subject and the session id claims separately
        return logoutTokenKeyValue + CACHE_KEY_SEPARATOR + sid;
    }

    private static boolean targetsSession(JsonObject logoutTokenJson, JsonObject idTokenJson) {
        String logoutTokenIss = logoutTokenJson.getString(Claims.iss.name());
        if (logoutTokenIss != null && !logoutTokenIss.equals(idTokenJson.getString(Claims.iss.name()))) {
            LOG.debug("Logout token issuer does not match the ID token issuer");
            return false;
        }
        String logoutTokenSub = logoutTokenJson.getString(Claims.sub.name());
        String idTokenSub = idTokenJson.getString(Claims.sub.name());
        if (logoutTokenSub != null && idTokenSub != null && !logoutTokenSub.equals(idTokenSub)) {
            LOG.debug("Logout token subject does not match the ID token subject");
            return false;
        }
        String logoutTokenSid = logoutTokenJson.getString(OidcConstants.BACK_CHANNEL_LOGOUT_SID_CLAIM);
        String idTokenSid = idTokenJson.getString(OidcConstants.ID_TOKEN_SID_CLAIM);
        if (logoutTokenSid != null && idTokenSid != null && !logoutTokenSid.equals(idTokenSid)) {
            LOG.debug("Logout token session id does not match the ID token session id");
            return false;
        }
        if (logoutTokenSid == null && isIdTokenIssuedAfterLogoutToken(idTokenJson, logoutTokenJson)) {
            LOG.trace("ID token was issued after the logout token, the session is not affected");
            return false;
        }
        return true;
    }

    private static boolean isIdTokenIssuedAfterLogoutToken(JsonObject idToken, JsonObject logoutToken) {
        return isIssuedAfter(idToken, logoutToken.getLong(Claims.iat.name()));
    }

    private static boolean isIssuedAfter(JsonObject token, Long issuedAt) {
        Long tokenIssuedAt = token.getLong(Claims.iat.name());
        return tokenIssuedAt != null && issuedAt != null && tokenIssuedAt > issuedAt;
    }
}
