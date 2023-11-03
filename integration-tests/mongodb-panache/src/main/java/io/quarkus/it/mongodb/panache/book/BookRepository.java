package io.quarkus.it.mongodb.panache.book;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class BookRepository implements PanacheMongoRepository<Book> {
}
