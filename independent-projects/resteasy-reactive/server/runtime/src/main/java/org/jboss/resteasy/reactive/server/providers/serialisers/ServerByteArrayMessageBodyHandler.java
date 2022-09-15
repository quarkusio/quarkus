package org.jboss.resteasy.reactive.server.providers.serialisers;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.common.providers.serialisers.ByteArrayMessageBodyHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.MessageReaderUtil;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Provider
public class ServerByteArrayMessageBodyHandler extends ByteArrayMessageBodyHandler
        implements ServerMessageBodyWriter<byte[]>, ServerMessageBodyReader<byte[]> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(byte[] o, Type genericType, ServerRequestContext context) throws WebApplicationException {
        // FIXME: use response encoding
        context.serverResponse().end(o);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return true;
    }

    @Override
    public byte[] readFrom(Class<byte[]> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        return MessageReaderUtil.readBytes(context.getInputStream());
    }
}
