package io.quarkus.it.mongodb.rest.data.panache;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;

@ApplicationScoped
public class BookRepository implements PanacheMongoRepositoryBase<Book, String> {
}
