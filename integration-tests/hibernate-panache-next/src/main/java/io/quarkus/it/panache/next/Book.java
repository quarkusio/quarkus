package io.quarkus.it.panache.next;

import java.util.List;

import jakarta.data.repository.Find;
import jakarta.persistence.Entity;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;

@Entity
public class Book extends PanacheEntity {
    public String title;
    public String author;
    public int pages;

    // Nested repository interface - Panache Next pattern with Jakarta Data finder methods
    public interface Repository extends PanacheRepository<Book> {
        // Jakarta Data finder methods annotated with @Find
        @Find
        List<Book> findByAuthor(String author);

        @Find
        Book findByTitle(String title);
    }
}
