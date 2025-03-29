package io.quarkus.vertx.http.runtime.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jboss.logging.Logger;

import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.ext.web.RoutingContext;

/**
 * A class that manages persistent logins.
 * This is done by encoding an expiry time, and the current username into an encrypted cookie
 * TODO: make this pluggable
 */
public class PersistentLoginManager {

    private static final Logger log = Logger.getLogger(PersistentLoginManager.class);
    private static final String ENC_ALGORITHM = "AES/GCM/NoPadding";
    private static final int ENC_TAG_LENGTH = 128;

    private final SecretKey secretKey;
    private final String cookieName;
    private final long timeoutMillis;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long newCookieIntervalMillis;
    private final boolean httpOnlyCookie;
    private final CookieSameSite cookieSameSite;
    private final String cookiePath;
    private final long maxAgeSeconds;

    public PersistentLoginManager(String encryptionKey, String cookieName, long timeoutMillis, long newCookieIntervalMillis,
            boolean httpOnlyCookie, String cookieSameSite, String cookiePath, long maxAgeSeconds) {
        this.cookieName = cookieName;
        this.newCookieIntervalMillis = newCookieIntervalMillis;
        this.timeoutMillis = timeoutMillis;
        this.httpOnlyCookie = httpOnlyCookie;
        this.cookieSameSite = CookieSameSite.valueOf(cookieSameSite);
        this.cookiePath = cookiePath;
        this.maxAgeSeconds = maxAgeSeconds;
        try {
            if (encryptionKey == null) {
                this.secretKey = KeyGenerator.getInstance("AES").generateKey();
            } else if (encryptionKey.length() < 16) {
                throw new RuntimeException("Shared keys for persistent logins must be more than 16 characters long");
            } else {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                sha256.update(encryptionKey.getBytes(StandardCharsets.UTF_8));
                this.secretKey = new SecretKeySpec(sha256.digest(), "AES");
            }
        } catch (Exception t) {
            throw new RuntimeException(t);
        }
    }

    public RestoreResult restore(RoutingContext context) {
        return restore(context, cookieName);
    }

    public RestoreResult restore(RoutingContext context, String cookieName) {
        Cookie existing = context.request().getCookie(cookieName);
        // If there is no credential cookie, we have nothing to restore.
        if (existing == null) {
            // Enforce new login.
            return null;
        }
        String val = existing.getValue();
        try {
            Cipher cipher = Cipher.getInstance(ENC_ALGORITHM);
            ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.getDecoder().decode(val.getBytes(StandardCharsets.UTF_8)));
            int ivLength = byteBuffer.get();
            byte[] iv = new byte[ivLength];
            byteBuffer.get(iv);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(ENC_TAG_LENGTH, iv));
            String result = new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
            int sep = result.indexOf(":");
            // If parsing fails, something is wrong and we need to enforce a new login.
            if (sep == -1) {
                // Enforce new login.
                log.debugf("%s cookie parsing failed. Is encryption-key set for all instances?", cookieName);
                return null;
            }
            long expireIdle = Long.parseLong(result.substring(0, sep));
            long now = System.currentTimeMillis();
            log.debugf("Current time: %s, Expire idle timeout: %s, expireIdle - now is: %d - %d = %d",
                    new Date(now).toString(), new Date(expireIdle).toString(), expireIdle, now, expireIdle - now);
            // We don't attempt renewal, idle timeout already expired.
            if (now > expireIdle) {
                // Enforce new login.
                return null;
            }
            boolean newCookieNeeded = (timeoutMillis - (expireIdle - now)) > newCookieIntervalMillis;
            log.debugf("Is new cookie needed? ( %d - ( %d - %d)) > %d : %b", timeoutMillis, expireIdle, now,
                    newCookieIntervalMillis, newCookieNeeded);
            return new RestoreResult(result.substring(sep + 1), newCookieNeeded);
        } catch (Exception e) {
            log.debug("Failed to restore persistent user session", e);
            return null;
        }
    }

    public void save(SecurityIdentity identity, RoutingContext context, RestoreResult restoreResult, boolean secureCookie) {
        save(identity.getPrincipal().getName(), context, cookieName, restoreResult, secureCookie);
    }

    public void save(String value, RoutingContext context, String cookieName, RestoreResult restoreResult,
            boolean secureCookie) {
        if (restoreResult != null) {
            if (!restoreResult.newCookieNeeded) {
                return;
            }
        }
        try {
            Cipher cipher = Cipher.getInstance(ENC_ALGORITHM);
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(ENC_TAG_LENGTH, iv));
            StringBuilder contents = new StringBuilder();
            long timeout = System.currentTimeMillis() + timeoutMillis;
            log.debugf("The new cookie will expire at %s", new Date(timeout).toString());
            contents.append(timeout);
            contents.append(":");
            contents.append(value);
            byte[] encrypted = cipher.doFinal(contents.toString().getBytes(StandardCharsets.UTF_8));
            ByteBuffer message = ByteBuffer.allocate(1 + iv.length + encrypted.length);
            message.put((byte) iv.length);
            message.put(iv);
            message.put(encrypted);
            String cookieValue = Base64.getEncoder().encodeToString(message.array());

            Cookie cookie = Cookie.cookie(cookieName, cookieValue)
                    .setPath(cookiePath)
                    .setSameSite(cookieSameSite)
                    .setSecure(secureCookie)
                    .setHttpOnly(httpOnlyCookie);
            if (maxAgeSeconds >= 0) {
                cookie.setMaxAge(maxAgeSeconds);
            }
            context.response().addCookie(cookie);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void clear(RoutingContext ctx) {
        // Vert.x sends back a set-cookie with max-age and expiry but no path, so we have to set it first,
        // otherwise web clients don't clear it
        Cookie cookie = ctx.request().getCookie(cookieName);
        if (cookie != null) {
            cookie.setPath("/");
        }
        ctx.response().removeCookie(cookieName);
    }

    String getCookieName() {
        return cookieName;
    }

    public static class RestoreResult {

        private final String principal;
        final boolean newCookieNeeded;

        public RestoreResult(String principal, boolean newCookieNeeded) {
            this.principal = principal;
            this.newCookieNeeded = newCookieNeeded;
        }

        public String getPrincipal() {
            return principal;
        }
    }

}
