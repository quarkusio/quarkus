package io.quarkus.it.mongodb.rest.data.panache;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class BookRepository implements PanacheMongoRepository<Book> {
}
