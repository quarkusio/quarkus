package org.jboss.resteasy.reactive.server.spi;

import java.io.InputStream;
import java.io.OutputStream;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.common.core.ResteasyReactiveCallbackContext;

public interface ServerRequestContext extends ResteasyReactiveCallbackContext {

    ServerHttpResponse serverResponse();

    InputStream getInputStream();

    ContentType getResponseContentType();

    MediaType getResponseMediaType();

    OutputStream getOrCreateOutputStream();

    ResteasyReactiveResourceInfo getResteasyReactiveResourceInfo();
}
