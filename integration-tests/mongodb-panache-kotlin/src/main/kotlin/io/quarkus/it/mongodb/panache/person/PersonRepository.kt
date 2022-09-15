package io.quarkus.it.mongodb.panache.person

import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepositoryBase
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class PersonRepository : PanacheMongoRepositoryBase<Person, Long>
