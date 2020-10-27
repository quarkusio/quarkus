package io.quarkus.rest.server.runtime.core;

import javax.ws.rs.core.MediaType;

/**
 * Wrapper around MediaType that saves the toString value, to avoid
 * the expensive header delegate processing.
 */
public class EncodedMediaType {
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

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getEncoded() {
        if (encoded == null) {
            return encoded = mediaType.toString();
        }
        return encoded;
    }

    public String getCharset() {
        if (charset == null) {
            return charset = mediaType.getParameters().get("charset");
        }
        return charset;
    }
}
