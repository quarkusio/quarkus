package org.jboss.resteasy.reactive.server.spi;

import java.io.IOException;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;

public interface ServerMessageBodyReader<T> extends MessageBodyReader<T> {

    boolean isReadable(Class<?> type, Type genericType,
            ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType);

    T readFrom(Class<T> type, Type genericType, MediaType mediaType,
            ServerRequestContext context) throws WebApplicationException, IOException;
}
