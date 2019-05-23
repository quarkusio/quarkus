package io.quarkus.tika;

public class Content {

    private String content;
    private Metadata metadata;

    public Content(String content, Metadata metadata) {
        this.content = content;
        this.metadata = metadata;
    }

    public String getText() {
        return content;
    }

    public Metadata getMetadata() {
        return metadata;
    }
}
