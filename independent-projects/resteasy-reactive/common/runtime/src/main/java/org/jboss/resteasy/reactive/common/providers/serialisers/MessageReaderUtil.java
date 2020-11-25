package org.jboss.resteasy.reactive.common.providers.serialisers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.MediaType;

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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024]; //TODO: fix, needs a pure vert.x async read model
        int r;
        while ((r = entityStream.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

    public static String readString(InputStream entityStream, MediaType mediaType) throws IOException {
        return new String(readBytes(entityStream), charsetFromMediaType(mediaType));
    }
}
