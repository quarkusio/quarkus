package io.quarkus.oidc.runtime;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

import io.quarkus.oidc.common.runtime.OidcConstants;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class BackChannelLogoutTokenCache {

    private static final Logger LOG = Logger.getLogger(BackChannelLogoutTokenCache.class);
    private static final String SESSION_KEY_SEPARATOR = "\u0000";

    private final MemoryCache<TokenVerificationResult> cache;
    private final String logoutTokenKey;
    private final boolean sessionIdKey;
    private final boolean logoutAllSessions;

    public BackChannelLogoutTokenCache(OidcTenantConfig oidcTenantConfig, Vertx vertx) {
        OidcTenantConfig.Backchannel backchannel = oidcTenantConfig.logout().backchannel();
        cache = new MemoryCache<>(vertx, backchannel.cleanUpTimerInterval(),
                backchannel.tokenCacheTimeToLive(), backchannel.tokenCacheSize());
        logoutTokenKey = backchannel.logoutTokenKey();
        sessionIdKey = OidcConstants.BACK_CHANNEL_LOGOUT_SID_CLAIM.equals(logoutTokenKey);
        logoutAllSessions = backchannel.logoutAllSessions();
    }

    public void addTokenVerification(TokenVerificationResult logoutToken) {
        JsonObject claims = logoutToken.localVerificationResult();
        String key = cacheKey(claims);
        TokenVerificationResult cachedLogoutToken = cache.get(key);
        if (cachedLogoutToken != null && issuedAfter(cachedLogoutToken.localVerificationResult(), claims)) {
            // logout tokens can arrive out of order, the latest one must win
            return;
        }
        cache.add(key, logoutToken);
    }

    public TokenVerificationResult findLogoutToken(JsonObject idTokenJson) {
        String keyValue = idTokenJson.getString(logoutTokenKey);
        if (keyValue == null) {
            return null;
        }
        String sessionKey = cacheKey(keyValue, idTokenJson.getString(OidcConstants.ID_TOKEN_SID_CLAIM));
        TokenVerificationResult logoutToken = findLogoutToken(sessionKey, idTokenJson);
        if (logoutToken == null && !sessionKey.equals(keyValue)) {
            // logout token without the session id targets all the sessions of the subject
            logoutToken = findLogoutToken(keyValue, idTokenJson);
        }
        return logoutToken;
    }

    public void logoutCompleted(TokenVerificationResult logoutToken) {
        JsonObject claims = logoutToken.localVerificationResult();
        if (logoutAllSessions && claims.getString(OidcConstants.BACK_CHANNEL_LOGOUT_SID_CLAIM) == null) {
            return;
        }
        cache.remove(cacheKey(claims));
    }

    void shutdown(Vertx vertx) {
        cache.stopTimer(vertx);
    }

    private TokenVerificationResult findLogoutToken(String key, JsonObject idTokenJson) {
        TokenVerificationResult logoutToken = cache.get(key);
        if (logoutToken != null && targetsSession(logoutToken.localVerificationResult(), idTokenJson)) {
            return logoutToken;
        }
        return null;
    }

    private String cacheKey(JsonObject logoutTokenJson) {
        return cacheKey(logoutTokenJson.getString(logoutTokenKey),
                logoutTokenJson.getString(OidcConstants.BACK_CHANNEL_LOGOUT_SID_CLAIM));
    }

    private String cacheKey(String keyValue, String sid) {
        if (sid == null || sessionIdKey) {
            return keyValue;
        }
        return keyValue + SESSION_KEY_SEPARATOR + sid;
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
        if (logoutTokenSid == null && issuedAfter(idTokenJson, logoutTokenJson)) {
            LOG.trace("ID token was issued after the logout token, the session is not affected");
            return false;
        }
        return true;
    }

    private static boolean issuedAfter(JsonObject idTokenJson, JsonObject logoutTokenJson) {
        Long idTokenIssuedAt = idTokenJson.getLong(Claims.iat.name());
        Long logoutTokenIssuedAt = logoutTokenJson.getLong(Claims.iat.name());
        return idTokenIssuedAt != null && logoutTokenIssuedAt != null && idTokenIssuedAt > logoutTokenIssuedAt;
    }
}
