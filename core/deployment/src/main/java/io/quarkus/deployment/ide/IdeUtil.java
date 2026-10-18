package io.quarkus.deployment.ide;

import java.util.Locale;

final class IdeUtil {

    private IdeUtil() {
    }

    static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os.toLowerCase(Locale.ENGLISH).startsWith("windows");
    }
}
