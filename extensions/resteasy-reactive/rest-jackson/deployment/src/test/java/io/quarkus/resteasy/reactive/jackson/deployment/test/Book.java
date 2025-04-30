package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Book {

    private String title;
    private String author;

    public Book() {
    }

    @JsonCreator
    public Book(String title, String author) {
        this.title = title;
        this.author = author;
    }

    public Book(String title) {
        this(title, null);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }
}