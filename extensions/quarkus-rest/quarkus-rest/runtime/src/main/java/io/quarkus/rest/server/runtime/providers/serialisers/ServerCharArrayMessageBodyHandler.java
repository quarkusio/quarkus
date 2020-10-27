package io.quarkus.rest.server.runtime.providers.serialisers;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import io.quarkus.rest.common.runtime.providers.serialisers.CharArrayMessageBodyHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.MessageReaderUtil;
import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyReader;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyWriter;

@Provider
public class ServerCharArrayMessageBodyHandler extends CharArrayMessageBodyHandler
        implements QuarkusRestMessageBodyWriter<char[]>, QuarkusRestMessageBodyReader<char[]> {

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(char[] o, QuarkusRestRequestContext context) throws WebApplicationException {
        // FIXME: use response encoding
        context.getHttpServerResponse().end(new String(o));
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return type.equals(String.class);
    }

    @Override
    public char[] readFrom(Class<char[]> type, Type genericType, MediaType mediaType, QuarkusRestRequestContext context)
            throws WebApplicationException, IOException {
        return MessageReaderUtil.readString(context.getInputStream(), mediaType).toCharArray();
    }
}
