package io.quarkus.it.data.hibernate;

import java.util.List;

import jakarta.data.repository.Find;
import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;

@Entity
public class Book extends ManagedEntity.AutoLong {
    public String title;
    public String author;
    public int pages;

    // Nested repository interface - Panache Next pattern with Jakarta Data finder methods
    public interface Repository extends ManagedRepository.AutoLong<Book> {
        // Jakarta Data finder methods annotated with @Find
        @Find
        List<Book> findByAuthor(String author);

        @Find
        Book findByTitle(String title);
    }
}
