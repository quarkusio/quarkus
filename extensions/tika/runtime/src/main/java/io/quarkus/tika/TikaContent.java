package io.quarkus.tika;

public class TikaContent {

    private String text;
    private TikaMetadata metadata;

    public TikaContent(String text, TikaMetadata metadata) {
        this.text = text;
        this.metadata = metadata;
    }

    public String getText() {
        return text;
    }

    public TikaMetadata getMetadata() {
        return metadata;
    }
}
