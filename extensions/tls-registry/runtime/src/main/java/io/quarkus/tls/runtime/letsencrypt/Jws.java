package io.quarkus.tls.runtime.letsencrypt;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

import io.vertx.core.json.JsonObject;

/**
 * Signs ACME requests with an EC P-256 account key, as a flattened JSON Web Signature with the {@code ES256}
 * algorithm (RFC 7515 and RFC 8555, section 6.2).
 */
final class Jws {

    private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();
    private static final int P256_COORDINATE_LENGTH = 32;

    private final KeyPair accountKey;
    private final JsonObject jwk;
    private final String thumbprint;

    /**
     * Create a signer for the given account key.
     *
     * @param accountKey an EC key pair on the P-256 curve
     * @throws GeneralSecurityException if the key is not a P-256 key
     */
    Jws(KeyPair accountKey) throws GeneralSecurityException {
        if (!(accountKey.getPublic() instanceof ECPublicKey publicKey)
                || publicKey.getParams().getCurve().getField().getFieldSize() != 256) {
            throw new GeneralSecurityException("The ACME account key must be an EC key on the P-256 curve");
        }
        this.accountKey = accountKey;
        String x = BASE64URL.encodeToString(unsigned(publicKey.getW().getAffineX()));
        String y = BASE64URL.encodeToString(unsigned(publicKey.getW().getAffineY()));
        this.jwk = new JsonObject().put("crv", "P-256").put("kty", "EC").put("x", x).put("y", y);
        // RFC 7638: the required members in lexicographic order, without white space
        String canonical = "{\"crv\":\"P-256\",\"kty\":\"EC\",\"x\":\"" + x + "\",\"y\":\"" + y + "\"}";
        this.thumbprint = BASE64URL.encodeToString(
                MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Get the RFC 7638 thumbprint of the account key, used in the key authorization of a challenge.
     *
     * @return the base64url thumbprint
     */
    String thumbprint() {
        return thumbprint;
    }

    /**
     * Get the public account key as a JSON Web Key.
     *
     * @return a copy of the key
     */
    JsonObject jwk() {
        return jwk.copy();
    }

    /**
     * Sign a request that identifies the account with its public key, as {@code newAccount} requires.
     *
     * @param url the request URL
     * @param nonce the replay nonce
     * @param payload the payload
     * @return the flattened JWS
     * @throws GeneralSecurityException if the signature fails
     */
    JsonObject signWithJwk(String url, String nonce, JsonObject payload) throws GeneralSecurityException {
        JsonObject header = new JsonObject().put("alg", "ES256").put("jwk", jwk()).put("nonce", nonce).put("url", url);
        return sign(header, payload.encode());
    }

    /**
     * Sign a request that identifies the account with its URL.
     *
     * @param url the request URL
     * @param nonce the replay nonce
     * @param accountUrl the account URL
     * @param payload the payload, or {@code null} for a POST-as-GET request
     * @return the flattened JWS
     * @throws GeneralSecurityException if the signature fails
     */
    JsonObject signWithKid(String url, String nonce, String accountUrl, JsonObject payload)
            throws GeneralSecurityException {
        JsonObject header = new JsonObject().put("alg", "ES256").put("kid", accountUrl).put("nonce", nonce)
                .put("url", url);
        return sign(header, payload == null ? "" : payload.encode());
    }

    private JsonObject sign(JsonObject header, String payload) throws GeneralSecurityException {
        String protectedHeader = BASE64URL.encodeToString(header.encode().getBytes(StandardCharsets.UTF_8));
        String encodedPayload = payload.isEmpty() ? ""
                : BASE64URL.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        // The P1363 format gives the R || S concatenation that JWS requires (RFC 7518, section 3.4)
        Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
        signature.initSign(accountKey.getPrivate());
        signature.update((protectedHeader + "." + encodedPayload).getBytes(StandardCharsets.US_ASCII));
        return new JsonObject()
                .put("protected", protectedHeader)
                .put("payload", encodedPayload)
                .put("signature", BASE64URL.encodeToString(signature.sign()));
    }

    private static byte[] unsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        byte[] result = new byte[P256_COORDINATE_LENGTH];
        int length = Math.min(bytes.length, P256_COORDINATE_LENGTH);
        System.arraycopy(bytes, bytes.length - length, result, P256_COORDINATE_LENGTH - length, length);
        return result;
    }
}
