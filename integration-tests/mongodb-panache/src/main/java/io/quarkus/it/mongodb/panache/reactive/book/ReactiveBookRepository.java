package io.quarkus.it.mongodb.panache.reactive.book;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.it.mongodb.panache.book.Book;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;

@ApplicationScoped
public class ReactiveBookRepository implements ReactivePanacheMongoRepository<Book> {
}
