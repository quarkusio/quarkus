package io.quarkus.it.mongo;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Book {

    private String id;

    private String title;
    private String author;

    private List<String> categories = new ArrayList<>();

    private BookDetail details;

    public String getTitle() {
        return title;
    }

    public Book setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getId() {
        return id;
    }

    public Book setId(String id) {
        this.id = id;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public Book setAuthor(String author) {
        this.author = author;
        return this;
    }

    public List<String> getCategories() {
        return categories;
    }

    public Book setCategories(List<String> categories) {
        this.categories = categories;
        return this;
    }

    public BookDetail getDetails() {
        return details;
    }

    public Book setDetails(BookDetail details) {
        this.details = details;
        return this;
    }
}
