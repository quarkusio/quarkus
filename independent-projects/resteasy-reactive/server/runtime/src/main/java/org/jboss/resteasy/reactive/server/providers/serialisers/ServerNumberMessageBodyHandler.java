package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.IOException;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.common.providers.serialisers.NumberMessageBodyHandler;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveMessageBodyReader;

@Provider
public class ServerNumberMessageBodyHandler extends NumberMessageBodyHandler
        implements ResteasyReactiveMessageBodyReader<Number> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return Number.class.isAssignableFrom(type);
    }

    @Override
    public Number readFrom(Class<Number> type, Type genericType, MediaType mediaType, ResteasyReactiveRequestContext context)
            throws WebApplicationException, IOException {
        return doReadFrom(type, context.getInputStream());
    }

}
