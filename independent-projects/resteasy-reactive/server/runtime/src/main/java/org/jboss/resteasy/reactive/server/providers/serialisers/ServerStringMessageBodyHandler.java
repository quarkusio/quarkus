package org.jboss.resteasy.reactive.server.providers.serialisers;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.common.providers.serialisers.StringMessageBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Provider
public class ServerStringMessageBodyHandler extends StringMessageBodyHandler
        implements ServerMessageBodyWriter<Object>, ServerMessageBodyReader<String> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Object o, Type genericType, ServerRequestContext context) throws WebApplicationException {
        // FIXME: use response encoding
        context.serverResponse().end(o.toString());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return type.equals(String.class);
    }

    @Override
    public String readFrom(Class<String> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        return readFrom(context.getInputStream(), true);
    }
}
