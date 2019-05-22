package io.quarkus.tika.runtime.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.tika.sax.ToTextContentHandler;

import io.quarkus.tika.TikaContent;

@Provider
public class TikaContentReader extends AbstractTikaReader<TikaContent> {

    public TikaContentReader() {
        super(TikaContent.class);
    }

    @Override
    public TikaContent readFrom(Class<TikaContent> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return createTikaContent(mediaType, entityStream, new ToTextContentHandler());
    }
}
