package io.quarkus.rest.server.runtime.providers.serialisers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.MessageReaderUtil;
import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.ReaderBodyHandler;

import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyReader;

public class ServerReaderBodyHandler extends ReaderBodyHandler implements QuarkusRestMessageBodyReader<Reader> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return type.equals(Reader.class);
    }

    @Override
    public Reader readFrom(Class<Reader> type, Type genericType, MediaType mediaType, QuarkusRestRequestContext context)
            throws WebApplicationException, IOException {
        return new InputStreamReader(context.getInputStream(), MessageReaderUtil.charsetFromMediaType(mediaType));
    }

    public long getSize(Reader inputStream, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

}
