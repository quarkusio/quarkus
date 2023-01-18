package io.quarkus.it.panache.kotlin

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
open class Bug5274EntityRepository : AbstractRepository<Person>()
