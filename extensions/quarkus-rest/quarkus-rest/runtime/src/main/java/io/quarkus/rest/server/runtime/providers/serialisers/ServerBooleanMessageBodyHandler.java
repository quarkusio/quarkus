package io.quarkus.rest.server.runtime.providers.serialisers;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.BooleanMessageBodyHandler;

import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyReader;

@Provider
public class ServerBooleanMessageBodyHandler extends BooleanMessageBodyHandler
        implements QuarkusRestMessageBodyReader<Boolean> {

    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return type == Boolean.class;
    }

    @Override
    public Boolean readFrom(Class<Boolean> type, Type genericType, MediaType mediaType, QuarkusRestRequestContext context)
            throws WebApplicationException, IOException {
        return Boolean.valueOf(super.readFrom(context.getInputStream(), false));
    }
}
