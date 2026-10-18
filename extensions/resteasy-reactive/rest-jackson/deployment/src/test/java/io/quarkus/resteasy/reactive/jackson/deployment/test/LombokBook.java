package io.quarkus.resteasy.reactive.jackson.deployment.test;

public class LombokBook {

    private String title;
    private String author;

    public String getTitle() {
        return title;
    }

    public LombokBook title(String title) {
        this.title = title;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public LombokBook author(String author) {
        this.author = author;
        return this;
    }
}