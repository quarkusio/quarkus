package io.quarkus.tika.runtime.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToTextContentHandler;
import org.xml.sax.ContentHandler;

import io.quarkus.tika.TikaContent;

@Provider
public class TikaMessageBodyReader implements MessageBodyReader<TikaContent> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == TikaContent.class;
    }

    @Override
    public TikaContent readFrom(Class<TikaContent> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return createTikaContent(mediaType, entityStream);
    }

    private TikaContent createTikaContent(MediaType mediaType, InputStream entityStream) throws IOException {
        Parser parser = new AutoDetectParser();
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);
        Metadata tikaMetadata = new Metadata();
        tikaMetadata.set(Metadata.CONTENT_TYPE, mediaType.toString());

        ContentHandler tikaHandler = new ToTextContentHandler();
        try (InputStream tikaStream = TikaInputStream.get(entityStream)) {
            parser.parse(tikaStream, tikaHandler, tikaMetadata, context);
            return new TikaContent(tikaHandler.toString().trim(), tikaMetadata);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
