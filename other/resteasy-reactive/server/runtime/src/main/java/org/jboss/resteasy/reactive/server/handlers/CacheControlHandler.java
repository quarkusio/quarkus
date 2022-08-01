package org.jboss.resteasy.reactive.server.handlers;

import javax.ws.rs.core.HttpHeaders;
import org.jboss.resteasy.reactive.common.util.ExtendedCacheControl;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class CacheControlHandler implements ServerRestHandler {

    // make mutable to allow for bytecode serialization
    private ExtendedCacheControl cacheControl;

    public CacheControlHandler() {
    }

    public ExtendedCacheControl getCacheControl() {
        return cacheControl;
    }

    public void setCacheControl(ExtendedCacheControl cacheControl) {
        this.cacheControl = cacheControl;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        requestContext.getResponse().get().getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, cacheControl);
    }
}
