package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.common.providers.serialisers.InputStreamMessageBodyHandler;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestMessageBodyReader;

@Provider
public class ServerInputStreamMessageBodyHandler extends InputStreamMessageBodyHandler
        implements QuarkusRestMessageBodyReader<InputStream> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return true;
    }

    @Override
    public InputStream readFrom(Class<InputStream> type, Type genericType, MediaType mediaType,
            ResteasyReactiveRequestContext context) throws WebApplicationException, IOException {
        return context.getInputStream();
    }

    @Override
    public long getSize(InputStream inputStream, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

}
