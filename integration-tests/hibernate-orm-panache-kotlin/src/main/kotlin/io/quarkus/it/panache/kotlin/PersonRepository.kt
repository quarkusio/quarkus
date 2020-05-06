package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.hibernate.orm.panache.kotlin.runtime.JpaOperations
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
open class PersonRepository : PanacheRepository<Person> {
    override fun count(query: String, params: Map<String, Any>): Long {
        return JpaOperations.count(Person::class.java, query, params)
    }
}