package io.quarkus.it.mongodb.panache.axle.book;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.it.mongodb.panache.book.Book;
import io.quarkus.mongodb.panache.axle.ReactivePanacheMongoRepository;

@ApplicationScoped
public class ReactiveBookRepository implements ReactivePanacheMongoRepository<Book> {
}
