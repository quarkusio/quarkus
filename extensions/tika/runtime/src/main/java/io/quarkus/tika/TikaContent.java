package io.quarkus.tika;

public class TikaContent {

    private String content;
    private Metadata metadata;

    public TikaContent(String content, Metadata metadata) {
        this.content = content;
    }

    public String getText() {
        return content;
    }

    public Metadata getMetadata() {
        return metadata;
    }
}
