package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcUtils.MAX_COOKIE_VALUE_LENGTH;
import static io.quarkus.oidc.runtime.OidcUtils.UNDERSCORE;
import static io.quarkus.oidc.runtime.OidcUtils.VOID_UNI;
import static io.quarkus.oidc.runtime.OidcUtils.createSessionCookie;
import static io.quarkus.oidc.runtime.OidcUtils.deleteTokensRequestContext;
import static io.quarkus.oidc.runtime.OidcUtils.getCookieSuffix;
import static io.quarkus.oidc.runtime.OidcUtils.removeCookie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Shutdown;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

@Dependent
public final class OidcHelper {

    static final String SESSION_COOKIE_CHUNK_START = "chunk_";
    static final String DEFAULT_COOKIE_PREFIX = "q_";
    private static final Logger LOG = Logger.getLogger(OidcHelper.class);
    private static final String ACCESS_TOKEN_COOKIE_SUFFIX = "_at";
    private static final String REFRESH_TOKEN_COOKIE_SUFFIX = "_rt";
    private static final String SESSION_COOKIE_CHUNK = "_" + SESSION_COOKIE_CHUNK_START;
    private static final String POST_LOGOUT = "post_logout";
    private static final String AUTH = "auth";
    private static final String SESSION = "session";

    private static volatile OidcHelper instance;

    private final String cookiePrefix;

    @Inject
    OidcHelper(OidcConfig oidcConfig) {
        this(oidcConfig.cookiePrefix());
    }

    private OidcHelper(String cookiePrefix) {
        this.cookiePrefix = cookiePrefix;
    }

    @Shutdown
    void shutdown() {
        // makes sure that we update the cookie prefix on each restart, so that config changes are reflected
        instance = null;
    }

    public static String getPostLogoutCookieName() {
        return getCookiePrefix() + POST_LOGOUT;
    }

    public static String getStateCookieName() {
        return getCookiePrefix() + AUTH;
    }

    public static String getSessionCookieName() {
        return getCookiePrefix() + SESSION;
    }

    public static String getSessionRtCookieName() {
        return getSessionCookieName() + REFRESH_TOKEN_COOKIE_SUFFIX;
    }

    public static String getSessionAtCookieName() {
        return getSessionCookieName() + ACCESS_TOKEN_COOKIE_SUFFIX;
    }

    public static String getSessionCookie(RoutingContext context, io.quarkus.oidc.OidcTenantConfig oidcTenantConfig) {
        final Map<String, Cookie> cookies = context.request().cookieMap();
        return getSessionCookie(context.data(), cookies, oidcTenantConfig);
    }

    public static String getSessionCookie(Map<String, Object> context, Map<String, Cookie> cookies,
            io.quarkus.oidc.OidcTenantConfig oidcTenantConfig) {
        return getSessionCookie(context, cookies, getSessionCookieName(), getSessionCookieName(oidcTenantConfig));
    }

    public static String getSessionCookie(Map<String, Object> context, Map<String, Cookie> cookies,
            String defaultSessionCookieName, String sessionCookieName) {
        if (cookies.isEmpty()) {
            return null;
        }

        if (cookies.containsKey(sessionCookieName)) {
            context.put(defaultSessionCookieName, List.of(sessionCookieName));
            return cookies.get(sessionCookieName).getValue();
        } else {
            final String sessionChunkPrefix = sessionCookieName + SESSION_COOKIE_CHUNK;

            SortedMap<String, String> sessionCookies = new TreeMap<>(new Comparator<String>() {

                @Override
                public int compare(String s1, String s2) {
                    // at this point it is guaranteed cookie names end with `chunk_<somenumber>`
                    int lastUnderscoreIndex1 = s1.lastIndexOf(UNDERSCORE);
                    int lastUnderscoreIndex2 = s2.lastIndexOf(UNDERSCORE);
                    Integer pos1 = Integer.valueOf(s1.substring(lastUnderscoreIndex1 + 1));
                    Integer pos2 = Integer.valueOf(s2.substring(lastUnderscoreIndex2 + 1));
                    return pos1.compareTo(pos2);
                }

            });
            for (String cookieName : cookies.keySet()) {
                if (cookieName.startsWith(sessionChunkPrefix)) {
                    sessionCookies.put(cookieName, cookies.get(cookieName).getValue());
                }
            }
            if (!sessionCookies.isEmpty()) {
                context.put(defaultSessionCookieName, new ArrayList<String>(sessionCookies.keySet()));

                StringBuilder sessionCookieValue = new StringBuilder();
                for (String value : sessionCookies.values()) {
                    sessionCookieValue.append(value);
                }
                return sessionCookieValue.toString();
            }
        }
        return null;
    }

    public static String getSessionCookieName(io.quarkus.oidc.OidcTenantConfig oidcConfig) {
        return getSessionCookieName() + getCookieSuffix(oidcConfig);
    }

    static Uni<Void> removeSessionCookie(RoutingContext context, OidcTenantConfig oidcConfig,
            TokenStateManager tokenStateManager) {
        List<String> cookieNames = context.get(getSessionCookieName());
        if (cookieNames != null) {
            LOG.debugf("Remove session cookie names: %s", cookieNames);
            StringBuilder cookieValue = new StringBuilder();
            for (String cookieName : cookieNames) {
                cookieValue.append(removeCookie(context, oidcConfig, cookieName));
            }
            return tokenStateManager.deleteTokens(context, oidcConfig, cookieValue.toString(),
                    deleteTokensRequestContext);
        } else {
            return VOID_UNI;
        }
    }

    public static boolean isSessionCookie(String cookieName) {
        String sessionCookieName = getSessionCookieName();
        return cookieName.startsWith(sessionCookieName)
                && !cookieName.regionMatches(sessionCookieName.length(), ACCESS_TOKEN_COOKIE_SUFFIX, 0, 3)
                && !cookieName.regionMatches(sessionCookieName.length(), REFRESH_TOKEN_COOKIE_SUFFIX, 0, 3);
    }

    static void createChunkedCookie(RoutingContext context, OidcTenantConfig oidcConfig, String baseCookieName,
            String cookieValue, long maxAge) {
        for (int chunkIndex = 1, currentPos = 0; currentPos < cookieValue.length(); chunkIndex++) {
            int nextPos = currentPos + MAX_COOKIE_VALUE_LENGTH;
            int nextValueUpperPos = nextPos < cookieValue.length() ? nextPos : cookieValue.length();
            String nextValue = cookieValue.substring(currentPos, nextValueUpperPos);
            // q_session_session_chunk_1, etc
            String nextName = baseCookieName + SESSION_COOKIE_CHUNK + chunkIndex;
            LOG.debugf("Creating the %s cookie chunk, size: %d", nextName, nextValue.length());
            createSessionCookie(context, oidcConfig, nextName, nextValue, maxAge);
            currentPos = nextPos;
        }
    }

    private static String getCookiePrefix() {
        return getInstance().cookiePrefix;
    }

    private static OidcHelper getInstance() {
        var currentInstance = OidcHelper.instance;
        if (currentInstance == null) {
            if (Arc.container() == null && LaunchMode.current().isDev()) {
                // this is mainly for OidcHelperTest, but it could be that in DEV mode, the container can be null temporarily
                currentInstance = new OidcHelper(DEFAULT_COOKIE_PREFIX);
            } else {
                try (var helperInstance = Arc.requireContainer().instance(OidcHelper.class)) {
                    currentInstance = OidcHelper.instance = helperInstance.get();
                }
            }
        }
        return currentInstance;
    }
}
