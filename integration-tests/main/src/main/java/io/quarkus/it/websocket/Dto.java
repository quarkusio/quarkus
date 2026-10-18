package io.quarkus.it.websocket;

public class Dto {
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Dto{" +
                "content='" + content + '\'' +
                '}';
    }
}
