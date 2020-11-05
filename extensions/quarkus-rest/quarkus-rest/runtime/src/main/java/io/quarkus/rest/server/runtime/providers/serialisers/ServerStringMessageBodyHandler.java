package io.quarkus.rest.server.runtime.providers.serialisers;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.StringMessageBodyHandler;

import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyReader;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyWriter;

@Provider
public class ServerStringMessageBodyHandler extends StringMessageBodyHandler
        implements QuarkusRestMessageBodyWriter<Object>, QuarkusRestMessageBodyReader<String> {

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Object o, QuarkusRestRequestContext context) throws WebApplicationException {
        // FIXME: use response encoding
        context.getHttpServerResponse().end(o.toString());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return type.equals(String.class);
    }

    @Override
    public String readFrom(Class<String> type, Type genericType, MediaType mediaType, QuarkusRestRequestContext context)
            throws WebApplicationException, IOException {
        return readFrom(context.getInputStream(), true);
    }
}
