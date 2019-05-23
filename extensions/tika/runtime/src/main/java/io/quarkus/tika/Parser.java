package io.quarkus.tika;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ToTextContentHandler;
import org.jboss.logging.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

public class Parser {
    private static final Logger LOG = Logger.getLogger(Parser.class.getName());

    public Content getContent(InputStream stream, String contentType) throws ParseException {
        return createTikaContent(stream, contentType, new ToTextContentHandler());
    }

    public io.quarkus.tika.Metadata getMetadata(InputStream stream, String contentType) throws ParseException {
        return createTikaContent(stream, contentType, contentType.contains("pdf") ? null : new DefaultHandler()).getMetadata();
    }

    protected Content createTikaContent(InputStream entityStream, String contentType, ContentHandler tikaHandler)
            throws ParseException {
        try {
            org.apache.tika.parser.Parser parser = new AutoDetectParser();
            ParseContext context = new ParseContext();
            context.set(org.apache.tika.parser.Parser.class, parser);
            org.apache.tika.metadata.Metadata tikaMetadata = new org.apache.tika.metadata.Metadata();
            tikaMetadata.set(Metadata.CONTENT_TYPE, contentType);

            try (InputStream tikaStream = TikaInputStream.get(entityStream)) {
                parser.parse(tikaStream, tikaHandler, tikaMetadata, context);
                return new Content(tikaHandler == null ? null : tikaHandler.toString().trim(), convert(tikaMetadata));
            }
        } catch (Exception e) {
            logExceptionTrace(e, contentType);
            throw new ParseException(e);
        }
    }

    private static void logExceptionTrace(Exception e, String contentType) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        LOG.warnf("%s stream can not be parsed: %s", contentType, sw.toString());
    }

    private static io.quarkus.tika.Metadata convert(org.apache.tika.metadata.Metadata tikaMetadata) {
        Map<String, List<String>> map = new HashMap<>();
        for (String name : tikaMetadata.names()) {
            map.put(name, Arrays.asList(tikaMetadata.getValues(name)));
        }
        return new io.quarkus.tika.Metadata(map);
    }
}
