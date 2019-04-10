package io.quarkus.tika.runtime.jaxrs;

public class TikaContent {

    private String content;

    public TikaContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
