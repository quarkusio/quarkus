package io.quarkus.rest.runtime.providers.serialisers;

import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.MediaType;

public class CharsetUtil {

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
}
