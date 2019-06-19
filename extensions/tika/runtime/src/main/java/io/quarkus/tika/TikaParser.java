package io.quarkus.tika;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToTextContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

@ApplicationScoped
public class TikaParser {

    public TikaContent parse(InputStream stream) throws TikaParseException {
        return parse(stream, null);
    }

    public TikaContent parse(InputStream stream, String contentType) throws TikaParseException {
        return parseStream(stream, contentType, new ToTextContentHandler());
    }

    public String getText(InputStream stream) throws TikaParseException {
        return parse(stream, null).getText();
    }

    public String getText(InputStream stream, String contentType) throws TikaParseException {
        return parseStream(stream, contentType, new ToTextContentHandler()).getText();
    }

    public io.quarkus.tika.TikaMetadata getMetadata(InputStream stream) throws TikaParseException {
        return getMetadata(stream, null);
    }

    public io.quarkus.tika.TikaMetadata getMetadata(InputStream stream, String contentType) throws TikaParseException {
        return parseStream(stream, contentType,
                contentType != null && contentType.contains("pdf") ? null : new DefaultHandler()).getMetadata();
    }

    protected TikaContent parseStream(InputStream entityStream, String contentType, ContentHandler tikaHandler)
            throws TikaParseException {
        try {
            Parser parser = createParser();
            ParseContext context = new ParseContext();
            context.set(Parser.class, parser);
            org.apache.tika.metadata.Metadata tikaMetadata = new org.apache.tika.metadata.Metadata();
            if (contentType != null) {
                tikaMetadata.set(Metadata.CONTENT_TYPE, contentType);
            }

            try (InputStream tikaStream = TikaInputStream.get(entityStream)) {
                parser.parse(tikaStream, tikaHandler, tikaMetadata, context);
                return new TikaContent(tikaHandler == null ? null : tikaHandler.toString().trim(), convert(tikaMetadata));
            }
        } catch (Exception e) {
            final String errorMessage = "Unable to parse the stream"
                    + (contentType == null ? "" : " for content-type: " + contentType);
            throw new TikaParseException(errorMessage, e);
        }
    }

    private Parser createParser() {
        // TODO Use the properties to support loading the format-specific and other generic parsers
        return new AutoDetectParser();
    }

    private static io.quarkus.tika.TikaMetadata convert(org.apache.tika.metadata.Metadata tikaMetadata) {
        Map<String, List<String>> map = new HashMap<>();
        for (String name : tikaMetadata.names()) {
            map.put(name, Arrays.asList(tikaMetadata.getValues(name)));
        }
        return new io.quarkus.tika.TikaMetadata(map);
    }
}
