package org.jboss.resteasy.reactive.server.spi;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.lang.reflect.Type;

public interface ServerMessageBodyReader<T> extends MessageBodyReader<T> {

    boolean isReadable(Class<?> type, Type genericType,
            ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType);

    T readFrom(Class<T> type, Type genericType, MediaType mediaType,
            ServerRequestContext context) throws WebApplicationException, IOException;
}
