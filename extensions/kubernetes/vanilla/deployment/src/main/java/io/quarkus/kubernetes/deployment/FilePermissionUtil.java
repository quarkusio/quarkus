package io.quarkus.kubernetes.deployment;

final class FilePermissionUtil {
    /**
     * When {@code value} starts with {@code 0}, parses it as an octal integer, otherwise as decimal.
     */
    static int parseInt(String value) {
        if (value.startsWith("0")) {
            return Integer.parseInt(value, 8);
        } else {
            return Integer.parseInt(value);
        }
    }
}
