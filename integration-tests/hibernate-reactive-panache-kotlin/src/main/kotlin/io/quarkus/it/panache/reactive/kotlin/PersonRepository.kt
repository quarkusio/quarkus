package io.quarkus.it.panache.reactive.kotlin


import io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepository
import io.quarkus.hibernate.reactive.panache.kotlin.runtime.KotlinJpaOperations.Companion.INSTANCE
import io.smallrye.mutiny.Uni
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
open class PersonRepository : PanacheRepository<Person> {
    override fun count(query: String, params: Map<String, Any>): Uni<Long> {
        return INSTANCE.count(Person::class.java, query, params)
    }
}
