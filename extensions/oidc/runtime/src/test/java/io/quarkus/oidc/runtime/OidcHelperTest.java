package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.runtime.LaunchMode;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.impl.CookieImpl;

class OidcHelperTest {

    @BeforeAll
    static void prepareOidcHelper() {
        // tell the OidcHelper it is alright that Arc container is null
        LaunchMode.set(LaunchMode.DEVELOPMENT);
    }

    @Test
    void testGetSingleSessionCookie() {
        OidcTenantConfig oidcConfig = OidcTenantConfig.builder().tenantId("test").build();
        Map<String, Object> context = new HashMap<>();
        String sessionCookieValue = OidcHelper.getSessionCookie(context,
                Map.of("q_session_test", new CookieImpl("q_session_test", "tokens")), oidcConfig);
        assertEquals("tokens", sessionCookieValue);
        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<String> names = (List) context.get(OidcHelper.getSessionCookieName());
        assertEquals(1, names.size());
        assertEquals("q_session_test", names.get(0));
    }

    @Test
    void testGetMultipleSessionCookies() {

        OidcTenantConfig oidcConfig = OidcTenantConfig.builder().tenantId("test").build();

        char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();

        StringBuilder expectedCookieValue = new StringBuilder();
        Map<String, Cookie> cookies = new HashMap<>();
        for (int i = 0; i < alphabet.length; i++) {
            char[] data = new char[OidcUtils.MAX_COOKIE_VALUE_LENGTH];
            Arrays.fill(data, alphabet[i]);
            String cookieName = "q_session_test_chunk_" + (i + 1);
            String nextChunk = new String(data);
            expectedCookieValue.append(nextChunk);
            cookies.put(cookieName, new CookieImpl(cookieName, nextChunk));
        }
        String lastChunk = String.valueOf("tokens");
        expectedCookieValue.append(lastChunk);
        String lastCookieName = "q_session_test_chunk_" + (alphabet.length + 1);
        cookies.put(lastCookieName, new CookieImpl(lastCookieName, lastChunk));

        Map<String, Object> context = new HashMap<>();
        String sessionCookieValue = OidcHelper.getSessionCookie(context, cookies, oidcConfig);
        assertEquals(expectedCookieValue.toString(), sessionCookieValue);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<String> names = (List) context.get(OidcHelper.getSessionCookieName());
        assertEquals(alphabet.length + 1, names.size());
        for (int i = 0; i < names.size(); i++) {
            assertEquals("q_session_test_chunk_" + (i + 1), names.get(i));
        }
    }

    @Test
    void testSessionCookieCheck() {
        assertTrue(OidcHelper.isSessionCookie(OidcHelper.getSessionCookieName()));
        assertTrue(OidcHelper.isSessionCookie(OidcHelper.getSessionCookieName() + "_tenant1"));
        assertFalse(OidcHelper.isSessionCookie(OidcHelper.getSessionAtCookieName()));
        assertFalse(OidcHelper.isSessionCookie(OidcHelper.getSessionAtCookieName() + "_tenant1"));
        assertFalse(OidcHelper.isSessionCookie(OidcHelper.getSessionRtCookieName()));
        assertFalse(OidcHelper.isSessionCookie(OidcHelper.getSessionRtCookieName() + "_tenant1"));

        assertFalse(OidcHelper.isSessionCookie(OidcHelper.getSessionAtCookieName() + "1"));
    }

    @Test
    void testGetSessionCookieTenantId() {
        assertEquals(OidcUtils.DEFAULT_TENANT_ID,
                OidcUtils.getTenantIdFromCookie(OidcHelper.getSessionCookieName(), "q_session", true));
        assertEquals(OidcUtils.DEFAULT_TENANT_ID,
                OidcUtils.getTenantIdFromCookie(OidcHelper.getSessionCookieName(), "q_session_chunk_1", true));
        assertEquals("a", OidcUtils.getTenantIdFromCookie(OidcHelper.getSessionCookieName(), "q_session_a", true));
        assertEquals("a", OidcUtils.getTenantIdFromCookie(OidcHelper.getSessionCookieName(), "q_session_a_chunk_1", true));
    }
}
