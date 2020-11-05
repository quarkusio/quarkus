package io.quarkus.rest.server.runtime.providers.serialisers;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.ByteArrayMessageBodyHandler;
import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.MessageReaderUtil;

import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyReader;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyWriter;
import io.vertx.core.buffer.Buffer;

@Provider
public class ServerByteArrayMessageBodyHandler extends ByteArrayMessageBodyHandler
        implements QuarkusRestMessageBodyWriter<byte[]>, QuarkusRestMessageBodyReader<byte[]> {

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(byte[] o, QuarkusRestRequestContext context) throws WebApplicationException {
        // FIXME: use response encoding
        context.getHttpServerResponse().end(Buffer.buffer(o));
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return true;
    }

    @Override
    public byte[] readFrom(Class<byte[]> type, Type genericType, MediaType mediaType, QuarkusRestRequestContext context)
            throws WebApplicationException, IOException {
        return MessageReaderUtil.readBytes(context.getInputStream());
    }
}
