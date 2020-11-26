package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.common.providers.serialisers.MessageReaderUtil;
import org.jboss.resteasy.reactive.common.providers.serialisers.ReaderBodyHandler;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveMessageBodyReader;

public class ServerReaderBodyHandler extends ReaderBodyHandler implements ResteasyReactiveMessageBodyReader<Reader> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return type.equals(Reader.class);
    }

    @Override
    public Reader readFrom(Class<Reader> type, Type genericType, MediaType mediaType, ResteasyReactiveRequestContext context)
            throws WebApplicationException, IOException {
        return new InputStreamReader(context.getInputStream(), MessageReaderUtil.charsetFromMediaType(mediaType));
    }

    public long getSize(Reader inputStream, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

}
