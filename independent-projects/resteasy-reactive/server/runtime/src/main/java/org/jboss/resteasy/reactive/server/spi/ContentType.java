package org.jboss.resteasy.reactive.server.spi;

import javax.ws.rs.core.MediaType;

public interface ContentType {

    String getCharset();

    String getEncoded();

    MediaType getMediaType();

}
