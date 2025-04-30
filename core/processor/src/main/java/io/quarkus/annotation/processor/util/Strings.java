package io.quarkus.annotation.processor.util;

public final class Strings {

    private Strings() {
    }

    public static boolean isBlank(String string) {
        return string == null || string.isBlank();
    }

    public static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }
}
