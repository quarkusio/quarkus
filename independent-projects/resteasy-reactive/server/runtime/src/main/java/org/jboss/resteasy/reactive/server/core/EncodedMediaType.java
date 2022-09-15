package org.jboss.resteasy.reactive.server.core;

import jakarta.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.jboss.resteasy.reactive.server.spi.ContentType;

/**
 * Wrapper around MediaType that saves the toString value, to avoid
 * the expensive header delegate processing.
 * It also harmonizes the use of charset
 */
public class EncodedMediaType implements ContentType {
    final MediaType mediaType;
    final String charset;
    String encoded;

    public EncodedMediaType(MediaType mediaType) {
        MediaType effectiveMediaType = mediaType;
        String effectiveCharset;
        String originalCharset = mediaType.getParameters().get("charset");
        if (isStringMediaType(mediaType)) {
            effectiveCharset = originalCharset;
            if (effectiveCharset == null) {
                effectiveCharset = StandardCharsets.UTF_8.name();
            }
        } else {
            // it doesn't make sense to add charset to non string types
            effectiveCharset = null;
        }
        this.charset = effectiveCharset;
        if (!Objects.equals(originalCharset, effectiveCharset)) {
            effectiveMediaType = mediaType.withCharset(effectiveCharset);
        }
        this.mediaType = effectiveMediaType;
    }

    // TODO: does this need to be more complex?
    private boolean isStringMediaType(MediaType mediaType) {
        String type = mediaType.getType();
        String subtype = mediaType.getSubtype();
        return (type.equals("application") && (subtype.contains("json") || subtype.contains("xml") || subtype.contains("yaml")))
                || type.equals("text");
    }

    @Override
    public String toString() {
        return getEncoded();
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public String getEncoded() {
        if (encoded == null) {
            return encoded = mediaType.toString();
        }
        return encoded;
    }

    @Override
    public String getCharset() {
        return charset;
    }
}
