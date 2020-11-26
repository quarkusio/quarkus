package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.IOException;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.common.providers.serialisers.CharacterMessageBodyHandler;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveMessageBodyReader;

@Provider
public class ServerCharacterMessageBodyHandler extends CharacterMessageBodyHandler
        implements ResteasyReactiveMessageBodyReader<Character> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return type == Character.class;
    }

    @Override
    public Character readFrom(Class<Character> type, Type genericType, MediaType mediaType,
            ResteasyReactiveRequestContext context)
            throws WebApplicationException, IOException {
        return doReadFrom(context.getInputStream());
    }

}
