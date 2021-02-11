package io.quarkus.tika;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.sax.AbstractRecursiveParserWrapperHandler;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.apache.tika.sax.RecursiveParserWrapperHandler;
import org.apache.tika.sax.ToTextContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

public class TikaParser {
    private Parser parser;
    private boolean appendEmbeddedContent;

    public TikaParser(Parser parser, boolean appendEmbeddedContent) {
        this.parser = parser;
        this.appendEmbeddedContent = appendEmbeddedContent;
    }

    public TikaContent parse(InputStream stream) throws TikaParseException {
        return parse(stream, (String) null);
    }

    public TikaContent parse(InputStream stream, ContentHandler contentHandler) throws TikaParseException {
        return parse(stream, null, validateContentHandler(contentHandler));
    }

    public TikaContent parse(InputStream stream, String contentType) throws TikaParseException {
        return parse(stream, contentType, createContentHandler());
    }

    public TikaContent parse(InputStream stream, String contentType, ContentHandler contentHandler) throws TikaParseException {
        return parseStream(stream, contentType, validateContentHandler(contentHandler));
    }

    public String getText(InputStream stream) throws TikaParseException {
        return parse(stream).getText();
    }

    public String getText(InputStream stream, ContentHandler contentHandler) throws TikaParseException {
        return parse(stream, validateContentHandler(contentHandler)).getText();
    }

    public String getText(InputStream stream, String contentType) throws TikaParseException {
        return parse(stream, contentType).getText();
    }

    public String getText(InputStream stream, String contentType, ContentHandler contentHandler) throws TikaParseException {
        return parse(stream, contentType, validateContentHandler(contentHandler)).getText();
    }

    public io.quarkus.tika.TikaMetadata getMetadata(InputStream stream) throws TikaParseException {
        return getMetadata(stream, null);
    }

    public io.quarkus.tika.TikaMetadata getMetadata(InputStream stream, String contentType) throws TikaParseException {
        return parseStream(stream, contentType, createContentHandlerForMetadataOnly(contentType)).getMetadata();
    }

    protected TikaContent parseStream(InputStream entityStream, String contentType, ContentHandler tikaHandler)
            throws TikaParseException {
        try {
            ParseContext context = new ParseContext();
            // AutoDetectParser must be set in the context to enable the parsing of the embedded content
            Parser contextParser = this.appendEmbeddedContent ? parser : ((RecursiveParserWrapper) parser).getWrappedParser();
            context.set(Parser.class, contextParser);

            org.apache.tika.metadata.Metadata tikaMetadata = new org.apache.tika.metadata.Metadata();
            if (contentType != null) {
                tikaMetadata.set(HttpHeaders.CONTENT_TYPE, contentType);
            }

            try (InputStream tikaStream = TikaInputStream.get(entityStream)) {
                parser.parse(tikaStream, tikaHandler, tikaMetadata, context);
                if (this.appendEmbeddedContent) {
                    // the embedded content if any has already been appended to the master content
                    return new TikaContent(tikaHandler == null ? null : tikaHandler.toString().trim(), convert(tikaMetadata));
                } else {
                    RecursiveParserWrapperHandler rHandler = (RecursiveParserWrapperHandler) tikaHandler;

                    // The metadata list represents the master and embedded content (text and metadata)
                    // The first metadata in the list represents the master (outer) content
                    List<org.apache.tika.metadata.Metadata> allMetadata = rHandler.getMetadataList();
                    String masterText = allMetadata.get(0).get(AbstractRecursiveParserWrapperHandler.TIKA_CONTENT);

                    // Embedded (inner) content starts from the index 1.
                    List<TikaContent> embeddedContent = new LinkedList<>();
                    for (int i = 1; i < allMetadata.size(); i++) {
                        String embeddedText = allMetadata.get(i).get(AbstractRecursiveParserWrapperHandler.TIKA_CONTENT);
                        // the embedded text can be null if the given document is an image
                        // and no text recognition parser is enabled
                        if (embeddedText != null) {
                            embeddedContent.add(new TikaContent(embeddedText.trim(), convert(allMetadata.get(i))));
                        }
                    }
                    return new TikaContent(masterText, convert(allMetadata.get(0)), embeddedContent);

                }
            }
        } catch (Exception e) {
            final String errorMessage = "Unable to parse the stream"
                    + (contentType == null ? "" : " for content-type: " + contentType);
            throw new TikaParseException(errorMessage, e);
        }
    }

    private ContentHandler validateContentHandler(ContentHandler contentHandler) {
        if (!this.appendEmbeddedContent && !(contentHandler instanceof RecursiveParserWrapperHandler)) {
            throw new IllegalStateException(
                    "The master and every embedded document will require a unique ContentHandler instance");
        }
        return contentHandler;
    }

    private ContentHandler createContentHandler() {
        // RecursiveParserWrapperHandler will use the factory to create a new ContentHandler
        // for the master and each of the embedded documents
        return this.appendEmbeddedContent ? new ToTextContentHandler()
                : new RecursiveParserWrapperHandler(
                        new BasicContentHandlerFactory(BasicContentHandlerFactory.HANDLER_TYPE.TEXT, -1));
    }

    private ContentHandler createContentHandlerForMetadataOnly(String contentType) {
        if (this.appendEmbeddedContent) {
            // PDF parser can completely skip processing the text if ContentHandler is null
            return contentType != null && contentType.contains("pdf") ? null : new DefaultHandler();
        } else {
            // Do not store the text; note the PDF format optimization is not possible in this case
            return new RecursiveParserWrapperHandler(
                    new BasicContentHandlerFactory(BasicContentHandlerFactory.HANDLER_TYPE.IGNORE, -1));
        }
    }

    private static io.quarkus.tika.TikaMetadata convert(org.apache.tika.metadata.Metadata tikaMetadata) {
        Map<String, List<String>> map = new HashMap<>();
        for (String name : tikaMetadata.names()) {
            map.put(name, Arrays.asList(tikaMetadata.getValues(name)));
        }
        return new io.quarkus.tika.TikaMetadata(map);
    }
}
