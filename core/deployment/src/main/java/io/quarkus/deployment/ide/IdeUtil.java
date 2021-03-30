package io.quarkus.deployment.ide;

import java.io.File;
import java.util.Locale;

final class IdeUtil {

    // copied from Java 9
    // TODO remove when we move to Java 11
    static final File NULL_FILE = new File(isWindows() ? "NUL" : "/dev/null");

    private IdeUtil() {
    }

    static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os.toLowerCase(Locale.ENGLISH).startsWith("windows");
    }
}
