package io.quarkus.it.mongodb.panache.bugs

import io.quarkus.it.mongodb.panache.book.Book
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class Bug5274EntityRepository : AbstractRepository<Book>()