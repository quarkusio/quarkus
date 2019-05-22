package io.quarkus.tika.runtime.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

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
        return mediaType.getSubtype().equals("pdf") ? null : new ContentHandler() {

            @Override
            public void setDocumentLocator(Locator locator) {
            }

            @Override
            public void startDocument() throws SAXException {
            }

            @Override
            public void endDocument() throws SAXException {
            }

            @Override
            public void startPrefixMapping(String prefix, String uri) throws SAXException {
            }

            @Override
            public void endPrefixMapping(String prefix) throws SAXException {
            }

            @Override
            public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
            }

            @Override
            public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            }

            @Override
            public void processingInstruction(String target, String data) throws SAXException {
            }

            @Override
            public void skippedEntity(String name) throws SAXException {
            }
        };
    }
}
