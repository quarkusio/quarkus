package io.quarkus.it.mongodb.panache.reactive.book

import io.quarkus.it.mongodb.panache.book.Book
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ReactiveBookRepository : ReactivePanacheMongoRepository<Book>
