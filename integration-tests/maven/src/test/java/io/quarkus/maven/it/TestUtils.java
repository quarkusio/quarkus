package io.quarkus.maven.it;

import io.smallrye.common.os.OS;

final class TestUtils {

    private static final long DEFAULT_TIMEOUT = OS.current() == OS.WINDOWS ? 3L : 1L;

    private TestUtils() {
    }

    static long getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }
}
