package io.quarkus.runtime.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtil {

    private static MessageDigest getMessageDigest(String alg) {
        try {
            return MessageDigest.getInstance(alg);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static void toHex(byte[] digest, StringBuilder sb) {
        for (int i = 0; i < digest.length; ++i) {
            sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100), 1, 3);
        }
    }

    private HashUtil() {
    }

    public static String sha1(String value) {
        return sha1(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha1(byte[] value) {
        final byte[] digest = getMessageDigest("SHA-1").digest(value);
        var sb = new StringBuilder(40);
        toHex(digest, sb);
        return sb.toString();
    }

    public static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] value) {
        final byte[] digest = getMessageDigest("SHA-256").digest(value);
        var sb = new StringBuilder(40);
        toHex(digest, sb);
        return sb.toString();
    }

    public static String sha512(String value) {
        return sha512(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha512(byte[] value) {
        final byte[] digest = getMessageDigest("SHA-512").digest(value);
        var sb = new StringBuilder(128);
        toHex(digest, sb);
        return sb.toString();
    }
}
