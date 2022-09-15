package org.jboss.resteasy.reactive.server.providers.serialisers;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.common.providers.serialisers.CharacterMessageBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Provider
public class ServerCharacterMessageBodyHandler extends CharacterMessageBodyHandler
        implements ServerMessageBodyReader<Character> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return type == Character.class;
    }

    @Override
    public Character readFrom(Class<Character> type, Type genericType, MediaType mediaType,
            ServerRequestContext context)
            throws WebApplicationException, IOException {
        return doReadFrom(context.getInputStream());
    }

}
