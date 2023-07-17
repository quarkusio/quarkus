package org.jboss.resteasy.reactive.server.spi;

import java.io.InputStream;
import java.io.OutputStream;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.common.core.ResteasyReactiveCallbackContext;

public interface ServerRequestContext extends ResteasyReactiveCallbackContext {

    ServerHttpResponse serverResponse();

    InputStream getInputStream();

    ContentType getResponseContentType();

    MediaType getResponseMediaType();

    OutputStream getOrCreateOutputStream();

    ResteasyReactiveResourceInfo getResteasyReactiveResourceInfo();

    void abortWith(Response response);
}
