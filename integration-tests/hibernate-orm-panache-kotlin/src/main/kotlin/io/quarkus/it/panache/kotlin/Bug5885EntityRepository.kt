package io.quarkus.it.panache.kotlin

import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
open class Bug5885EntityRepository : Bug5885AbstractRepository<Person>()