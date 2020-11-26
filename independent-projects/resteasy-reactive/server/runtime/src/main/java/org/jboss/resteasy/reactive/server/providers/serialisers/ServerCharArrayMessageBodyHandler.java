package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.IOException;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.common.providers.serialisers.CharArrayMessageBodyHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.MessageReaderUtil;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveMessageBodyWriter;

@Provider
public class ServerCharArrayMessageBodyHandler extends CharArrayMessageBodyHandler
        implements ResteasyReactiveMessageBodyWriter<char[]>, ResteasyReactiveMessageBodyReader<char[]> {

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(char[] o, ResteasyReactiveRequestContext context) throws WebApplicationException {
        // FIXME: use response encoding
        context.serverResponse().end(new String(o));
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return type.equals(String.class);
    }

    @Override
    public char[] readFrom(Class<char[]> type, Type genericType, MediaType mediaType, ResteasyReactiveRequestContext context)
            throws WebApplicationException, IOException {
        return MessageReaderUtil.readString(context.getInputStream(), mediaType).toCharArray();
    }
}
