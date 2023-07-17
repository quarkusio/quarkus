package org.jboss.resteasy.reactive.common.providers.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.MediaType;

public class MessageReaderUtil {

    public static final String UTF8_CHARSET = StandardCharsets.UTF_8.name();

    public static String charsetFromMediaType(MediaType mediaType) {
        if (mediaType == null) {
            return UTF8_CHARSET;
        }
        String charset = mediaType.getParameters().get(MediaType.CHARSET_PARAMETER);
        if (charset != null) {
            return charset;
        }
        return UTF8_CHARSET;
    }

    public static byte[] readBytes(InputStream entityStream) throws IOException {
        return entityStream.readAllBytes();
    }

    public static String readString(InputStream entityStream, MediaType mediaType) throws IOException {
        return new String(readBytes(entityStream), charsetFromMediaType(mediaType));
    }
}
