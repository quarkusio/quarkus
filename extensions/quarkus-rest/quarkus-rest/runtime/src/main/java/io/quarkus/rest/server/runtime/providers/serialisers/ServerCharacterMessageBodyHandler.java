package io.quarkus.rest.server.runtime.providers.serialisers;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.CharacterMessageBodyHandler;

import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyReader;

@Provider
public class ServerCharacterMessageBodyHandler extends CharacterMessageBodyHandler
        implements QuarkusRestMessageBodyReader<Character> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return type == Character.class;
    }

    @Override
    public Character readFrom(Class<Character> type, Type genericType, MediaType mediaType, QuarkusRestRequestContext context)
            throws WebApplicationException, IOException {
        return doReadFrom(context.getInputStream());
    }

}
