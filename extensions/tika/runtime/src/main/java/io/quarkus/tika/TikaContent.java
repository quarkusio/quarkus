package io.quarkus.tika;

import org.apache.tika.metadata.Metadata;

public class TikaContent {

    private String content;
    private Metadata metadata;

    public TikaContent(String content, Metadata metadata) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public Metadata getMetadata() {
        return metadata;
    }
}
