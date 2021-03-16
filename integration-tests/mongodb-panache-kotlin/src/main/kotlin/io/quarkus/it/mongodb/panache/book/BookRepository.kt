package io.quarkus.it.mongodb.panache.book

import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class BookRepository : PanacheMongoRepository<Book>