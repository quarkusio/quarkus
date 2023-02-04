package io.quarkus.it.panache.reactive.kotlin

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class Bug5885EntityRepository : Bug5885AbstractRepository<Person>()
