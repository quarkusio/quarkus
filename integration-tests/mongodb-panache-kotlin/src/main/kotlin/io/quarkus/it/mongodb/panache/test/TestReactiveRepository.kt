package io.quarkus.it.mongodb.panache.test

import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepository
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class TestReactiveRepository : ReactivePanacheMongoRepository<TestReactiveEntity>