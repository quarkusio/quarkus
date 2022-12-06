package io.quarkus.it.panache.reactive.kotlin

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class Bug5274EntityRepository : AbstractRepository<Person>()
