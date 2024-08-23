package io.quarkus.it.mongodb.panache.book

import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class BookRepository(private val dummyService: DummyService) : PanacheMongoRepository<Book>
