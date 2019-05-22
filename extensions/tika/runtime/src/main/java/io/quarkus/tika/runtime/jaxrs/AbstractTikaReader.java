package io.quarkus.tika.runtime.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.xml.sax.ContentHandler;

import io.quarkus.tika.TikaContent;

public abstract class AbstractTikaReader<T> implements MessageBodyReader<T> {
    private Class<T> tikaClass;

    protected AbstractTikaReader(Class<T> tikaClass) {
        this.tikaClass = tikaClass;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return tikaClass == type;
    }

    protected TikaContent createTikaContent(MediaType mediaType, InputStream entityStream, ContentHandler tikaHandler)
            throws IOException {
        Parser parser = new AutoDetectParser();
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);
        org.apache.tika.metadata.Metadata tikaMetadata = new org.apache.tika.metadata.Metadata();
        tikaMetadata.set(Metadata.CONTENT_TYPE, mediaType.toString());

        try (InputStream tikaStream = TikaInputStream.get(entityStream)) {
            parser.parse(tikaStream, tikaHandler, tikaMetadata, context);
            return new TikaContent(tikaHandler == null ? null : tikaHandler.toString().trim(), convert(tikaMetadata));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return new TikaContent(sw.toString(), null);
            //throw new IOException(e);
        }
    }

    protected io.quarkus.tika.Metadata convert(org.apache.tika.metadata.Metadata tikaMetadata) {
        Map<String, List<String>> map = new HashMap<>();
        for (String name : tikaMetadata.names()) {
            map.put(name, Arrays.asList(tikaMetadata.getValues(name)));
        }
        return new io.quarkus.tika.Metadata(map);
    }

}
