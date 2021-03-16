package io.quarkus.tika;

import java.util.Collections;
import java.util.List;

public class TikaContent {

    private String text;
    private TikaMetadata metadata;
    private List<TikaContent> embeddedContent;

    public TikaContent(String text, TikaMetadata metadata) {
        this(text, metadata, Collections.emptyList());
    }

    public TikaContent(String text, TikaMetadata metadata, List<TikaContent> embeddedContent) {
        this.text = text;
        this.metadata = metadata;
        this.embeddedContent = embeddedContent;
    }

    /**
     * Return the document text.
     * The text of the embedded documents if any will be appended to this text unless
     * the parser has been configured to provide the content of the embedded documents separately
     *
     * @return the document text
     */
    public String getText() {
        return text;
    }

    /**
     * Return the document metadata.
     *
     * @return the metadata
     */
    public TikaMetadata getMetadata() {
        return metadata;
    }

    /**
     * Return the content of the embedded documents
     *
     * @return the list of the embedded documents, will be empty
     *         if the current document has no embedded content or it has been appended to the main content
     */
    public List<TikaContent> getEmbeddedContent() {
        return embeddedContent;
    }
}
