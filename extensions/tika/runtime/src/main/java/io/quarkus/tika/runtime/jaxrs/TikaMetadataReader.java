package io.quarkus.tika.runtime.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import io.quarkus.tika.Metadata;

@Provider
public class TikaMetadataReader extends AbstractTikaReader<Metadata> {

    public TikaMetadataReader() {
        super(Metadata.class);
    }

    @Override
    public Metadata readFrom(Class<Metadata> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return createTikaContent(mediaType, entityStream, getContentHandler(mediaType)).getMetadata();
    }

    private ContentHandler getContentHandler(MediaType mediaType) {
        // PDFParser is optimized to return the metadata only if no ContentHandler is set.
        // In all other cases use the handler which ignores the reported text.
        return mediaType.getSubtype().equals("pdf") ? null : new DefaultHandler();
    }
}
