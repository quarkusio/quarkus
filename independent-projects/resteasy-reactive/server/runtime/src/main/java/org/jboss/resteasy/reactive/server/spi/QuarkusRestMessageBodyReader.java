package org.jboss.resteasy.reactive.server.spi;

import java.io.IOException;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public interface QuarkusRestMessageBodyReader<T> extends MessageBodyReader<T> {

    boolean isReadable(Class<?> type, Type genericType,
            LazyMethod lazyMethod, MediaType mediaType);

    T readFrom(Class<T> type, Type genericType, MediaType mediaType,
            ResteasyReactiveRequestContext context) throws WebApplicationException, IOException;
}
