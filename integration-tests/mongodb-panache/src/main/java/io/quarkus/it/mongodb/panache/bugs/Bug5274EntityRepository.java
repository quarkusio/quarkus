package io.quarkus.it.mongodb.panache.bugs;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.it.mongodb.panache.book.Book;

@ApplicationScoped
public class Bug5274EntityRepository extends AbstractRepository<Book> {
}
