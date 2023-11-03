package io.quarkus.arc.processor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

final class Hashes {

    /**
     * A hashing function that uses {@link MessageDigest} and {@link Base64} to encode a {@link String} input.
     * Arc doesn't need SHA-1, we just need a deterministic and unique String that's as short as possible because
     * resulting String values are used as keys and for equals checks.
     * <p>
     * We deliberately use Base64 URL variant, which in addition to A-Za-z0-9 uses the - and _ chars.
     * We also disable padding, so there's no =.
     * This is important, because the generated strings are also used as identifiers in class files.
     *
     * @param value String for encoding
     * @return Unique and deterministic String identifier, encoded with Base64 URL encoder
     */
    static String sha1_base64(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
