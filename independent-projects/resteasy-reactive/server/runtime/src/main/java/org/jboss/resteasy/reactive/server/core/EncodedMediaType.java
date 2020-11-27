package org.jboss.resteasy.reactive.server.core;

import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.server.spi.ContentType;

/**
 * Wrapper around MediaType that saves the toString value, to avoid
 * the expensive header delegate processing.
 */
public class EncodedMediaType implements ContentType {
    final MediaType mediaType;
    String encoded;
    String charset;

    public EncodedMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
        this.charset = mediaType.getParameters().get("charset");
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
        if (charset == null) {
            return charset = mediaType.getParameters().get("charset");
        }
        return charset;
    }
}
