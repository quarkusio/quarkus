package io.quarkus.resteasy.reactive.server.runtime;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jboss.resteasy.reactive.common.util.EmptyInputStream;

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
