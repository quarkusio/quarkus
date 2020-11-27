package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.IOException;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
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
    public boolean isWriteable(Class<?> type, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(byte[] o, ServerRequestContext context) throws WebApplicationException {
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
