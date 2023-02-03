package io.quarkus.it.mongodb.panache.test

import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class TestImperativeRepository : PanacheMongoRepository<TestImperativeEntity>
