package io.quarkus.test.common;

import java.util.Locale;

public class JdkUtil {

    public static boolean isSemeru() {
        return System.getProperty("java.runtime.name", "").toLowerCase(Locale.ROOT).contains("semeru");
    }
}
