package io.quarkus.it.hibernate.orm.rest.data.panache.common;

import java.util.LinkedList;
import java.util.List;

public class BookDto {

    public final Long id;

    public final String title;

    public final AuthorDto author;

    public final List<ReviewDto> reviews;

    public BookDto(String title, AuthorDto author) {
        this.id = null;
        this.title = title;
        this.author = author;
        this.reviews = new LinkedList<>();
    }

    public BookDto(Long id, String title, AuthorDto author) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.reviews = new LinkedList<>();
    }

    public BookDto(Long id, String title, AuthorDto author, List<ReviewDto> reviews) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.reviews = reviews;
    }
}