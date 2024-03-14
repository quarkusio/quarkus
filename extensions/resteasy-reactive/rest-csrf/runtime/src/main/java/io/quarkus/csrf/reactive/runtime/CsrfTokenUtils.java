package io.quarkus.csrf.reactive.runtime;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.jboss.logging.Logger;

public class CsrfTokenUtils {
    private static final Logger LOG = Logger.getLogger(CsrfTokenUtils.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private CsrfTokenUtils() {
    }

    public static String signCsrfToken(String encodedCsrfToken, String secretKey) {
        return signCsrfToken(Base64.getUrlDecoder().decode(encodedCsrfToken), secretKey);
    }

    public static String signCsrfToken(byte[] csrfToken, String secretKey) {
        if (secretKey.length() < 32) {
            LOG.error("Secret keys for signing CSRF tokens must be at least 32 characters long");
            throw new RuntimeException();
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), HMAC_SHA256);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKeySpec);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(csrfToken));
        } catch (InvalidKeyException ex) {
            LOG.error("Invalid secret key for signing the CSRF token");
            throw new RuntimeException();
        } catch (NoSuchAlgorithmException ex) {
            LOG.error("Invalid algorithm for signing the CSRF token");
            throw new RuntimeException();
        }

    }
}
