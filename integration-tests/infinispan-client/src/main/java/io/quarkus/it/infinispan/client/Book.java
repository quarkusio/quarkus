package io.quarkus.it.infinispan.client;

import java.util.Objects;
import java.util.Set;

/**
 * @author William Burns
 */
public class Book {
    private final String title;
    private final String description;
    private final int publicationYear;
    private final Set<Author> authors;
    private final Type bookType;

    enum Type {
        FANTASY,
        PROGRAMMING
    }

    public Book(String title, String description, int publicationYear, Set<Author> authors, Type bookType) {
        this.title = Objects.requireNonNull(title);
        this.description = Objects.requireNonNull(description);
        this.publicationYear = publicationYear;
        this.authors = Objects.requireNonNull(authors);
        this.bookType = bookType;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public Set<Author> getAuthors() {
        return authors;
    }

    public Type getBookType() {
        return bookType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Book book = (Book) o;
        return publicationYear == book.publicationYear &&
                title.equals(book.title) &&
                description.equals(book.description) &&
                authors.equals(book.authors) &&
                bookType.equals(book.bookType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, publicationYear, authors, bookType);
    }
}
