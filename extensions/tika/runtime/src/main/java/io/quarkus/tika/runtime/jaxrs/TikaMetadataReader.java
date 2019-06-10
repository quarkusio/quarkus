package io.quarkus.tika.runtime.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import io.quarkus.tika.Metadata;
import io.quarkus.tika.TikaParser;

@Provider
public class TikaMetadataReader extends AbstractTikaReader<Metadata> {

    public TikaMetadataReader() {
        super(Metadata.class);
    }

    @Override
    public Metadata readFrom(Class<Metadata> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return new TikaParser().getMetadata(entityStream, mediaType.toString());
    }
}
