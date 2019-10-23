package io.quarkus.vertx.http.runtime.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
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

    private final SecretKey secretKey;
    private final String cookieName;
    private final long timeoutMillis;

    public PersistentLoginManager(String encryptionKey, String cookieName, long timeoutMillis) {
        try {
            this.cookieName = cookieName;
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
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            String result = new String(cipher.doFinal(Base64.getDecoder().decode(val)), StandardCharsets.UTF_8);
            int sep = result.indexOf(":");
            if (sep == -1) {
                return null;
            }
            long expire = Long.parseLong(result.substring(0, sep));
            if (System.currentTimeMillis() > expire) {
                return null;
            }
            return new RestoreResult(result.substring(sep + 1), (System.currentTimeMillis() - expire) > 1000 * 60); //new cookie every minute
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
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            StringBuilder contents = new StringBuilder();
            //TODO: do we need random padding?
            long timeout = System.currentTimeMillis() + timeoutMillis;
            contents.append(timeout);
            contents.append(":");
            contents.append(identity.getPrincipal().getName());

            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            String cookieValue = Base64.getEncoder()
                    .encodeToString(cipher.doFinal(contents.toString().getBytes(StandardCharsets.UTF_8)));
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
