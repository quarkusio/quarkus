package org.jboss.resteasy.reactive.server.spi;

import jakarta.ws.rs.core.MediaType;

public interface ContentType {

    String getCharset();

    String getEncoded();

    MediaType getMediaType();

}
