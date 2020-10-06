package io.quarkus.smallrye.graphql.deployment;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class Book {
    public String isbn;
    public String title;
    public LocalDate published;
    public List<String> authors;

    public Book() {
    }

    public Book(String isbn, String title, LocalDate published, String... authors) {
        this.isbn = isbn;
        this.title = title;
        this.published = published;
        this.authors = Arrays.asList(authors);
    }

}
