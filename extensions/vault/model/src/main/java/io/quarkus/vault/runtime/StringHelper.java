package io.quarkus.vault.runtime;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StringHelper {

    public static byte[] stringToBytes(String s) {
        return s == null ? null : s.getBytes(UTF_8);
    }

    public static String bytesToString(byte[] bytes) {
        return bytes == null ? null : new String(bytes, UTF_8);
    }

}
