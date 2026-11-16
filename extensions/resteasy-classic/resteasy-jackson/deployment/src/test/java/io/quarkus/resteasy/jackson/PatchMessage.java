package io.quarkus.resteasy.jackson;

public class PatchMessage {

    private String message;
    private String author;

    public String getMessage() {
        return message;
    }

    public PatchMessage setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public PatchMessage setAuthor(String author) {
        this.author = author;
        return this;
    }
}
