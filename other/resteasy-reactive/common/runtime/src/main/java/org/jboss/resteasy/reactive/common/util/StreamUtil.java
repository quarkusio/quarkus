package org.jboss.resteasy.reactive.common.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public final class StreamUtil {

    private StreamUtil() {
    }

    public static boolean isEmpty(InputStream stream) {
        if (stream instanceof ByteArrayInputStream) {
            return (((ByteArrayInputStream) stream).available() == 0);
        }
        return stream instanceof EmptyInputStream;
    }
}
