package io.quarkus.it.mongodb.panache.person

import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepositoryBase
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
open class MockablePersonRepository : PanacheMongoRepositoryBase<PersonEntity, Long> {
    open fun findOrdered(): List<PersonEntity>? {
        val sort = Sort.by("lastname", "firstname")
        val found = findAll(sort)
        return found.list()
    }
}
