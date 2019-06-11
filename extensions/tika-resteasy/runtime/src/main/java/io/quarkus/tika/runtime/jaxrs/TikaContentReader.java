package io.quarkus.tika.runtime.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import io.quarkus.tika.Content;
import io.quarkus.tika.TikaParser;

@Provider
public class TikaContentReader extends AbstractTikaReader<Content> {

    public TikaContentReader() {
        super(Content.class);
    }

    @Override
    public Content readFrom(Class<Content> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return new TikaParser().getContent(entityStream, mediaType.toString());
    }
}
