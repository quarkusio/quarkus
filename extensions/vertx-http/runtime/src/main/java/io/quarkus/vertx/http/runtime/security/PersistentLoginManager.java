package io.quarkus.vertx.http.runtime.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jboss.logging.Logger;

import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

/**
 * A class that manages persistent logins.
 *
 * This is done by encoding an expiry time, and the current username into an encrypted cookie
 *
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
    private final long newCookieMillis;

    public PersistentLoginManager(String encryptionKey, String cookieName, long timeoutMillis, long newCookieMillis) {
        try {
            this.cookieName = cookieName;
            this.newCookieMillis = newCookieMillis;
            this.timeoutMillis = timeoutMillis;
            if (encryptionKey == null) {
                secretKey = KeyGenerator.getInstance("AES").generateKey();
            } else if (encryptionKey.length() < 16) {
                throw new RuntimeException("Shared keys for persistent logins must be more than 16 characters long");
            } else {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                sha256.update(encryptionKey.getBytes(StandardCharsets.UTF_8));
                secretKey = new SecretKeySpec(sha256.digest(), "AES");
            }
        } catch (Exception t) {
            throw new RuntimeException(t);
        }

    }

    public RestoreResult restore(RoutingContext context) {
        Cookie existing = context.getCookie(cookieName);
        if (existing == null) {
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
            if (sep == -1) {
                return null;
            }
            long expire = Long.parseLong(result.substring(0, sep));
            if (System.currentTimeMillis() > expire) {
                return null;
            }
            return new RestoreResult(result.substring(sep + 1), (System.currentTimeMillis() - expire) > newCookieMillis);
        } catch (Exception e) {
            log.debug("Failed to restore persistent user session", e);
            return null;
        }
    }

    public void save(SecurityIdentity identity, RoutingContext context, RestoreResult restoreResult) {
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
            contents.append(timeout);
            contents.append(":");
            contents.append(identity.getPrincipal().getName());
            byte[] encrypted = cipher.doFinal(contents.toString().getBytes(StandardCharsets.UTF_8));
            ByteBuffer message = ByteBuffer.allocate(1 + iv.length + encrypted.length);
            message.put((byte) iv.length);
            message.put(iv);
            message.put(encrypted);
            String cookieValue = Base64.getEncoder().encodeToString(message.array());
            context.addCookie(Cookie.cookie(cookieName, cookieValue).setPath("/"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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
