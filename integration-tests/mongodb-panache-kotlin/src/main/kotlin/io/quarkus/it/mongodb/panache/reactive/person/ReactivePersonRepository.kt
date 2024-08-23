package io.quarkus.it.mongodb.panache.reactive.person

import io.quarkus.it.mongodb.panache.person.Person
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepositoryBase
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ReactivePersonRepository : ReactivePanacheMongoRepositoryBase<Person, Long>
