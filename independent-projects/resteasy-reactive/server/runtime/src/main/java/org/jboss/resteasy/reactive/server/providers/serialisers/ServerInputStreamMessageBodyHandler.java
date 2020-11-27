package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.common.providers.serialisers.InputStreamMessageBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Provider
public class ServerInputStreamMessageBodyHandler extends InputStreamMessageBodyHandler
        implements ServerMessageBodyReader<InputStream> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return true;
    }

    @Override
    public InputStream readFrom(Class<InputStream> type, Type genericType, MediaType mediaType,
            ServerRequestContext context) throws WebApplicationException, IOException {
        return context.getInputStream();
    }

    @Override
    public long getSize(InputStream inputStream, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

}
