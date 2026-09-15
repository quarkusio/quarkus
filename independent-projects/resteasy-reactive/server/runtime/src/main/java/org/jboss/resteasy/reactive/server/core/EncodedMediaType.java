package org.jboss.resteasy.reactive.server.core;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
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
        if (isJson(mediaType)) {
            effectiveCharset = originalCharset;
        } else if (MediaTypeHelper.isTextLike(mediaType)) {
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

    /**
     * JSON text is always encoded in UTF-8, UTF-16 or UTF-32 (RFC 8259) and the {@code application/json} media type
     * registration defines no {@code charset} parameter, so a charset is only kept if it was set explicitly.
     *
     * @return {@code true} for {@code application/json} and the {@code +json} structured syntax suffix
     */
    private static boolean isJson(MediaType mediaType) {
        if (!"application".equals(mediaType.getType())) {
            return false;
        }
        String subtype = mediaType.getSubtype();
        return "json".equals(subtype) || subtype.endsWith("+json");
    }

    // TODO: does this need to be more complex?

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
            return encoded = MediaTypeHelper.toString(mediaType);
        }
        return encoded;
    }

    @Override
    public String getCharset() {
        return charset;
    }
}
