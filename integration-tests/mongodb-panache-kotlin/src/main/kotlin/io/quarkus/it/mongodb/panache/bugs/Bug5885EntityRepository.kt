package io.quarkus.it.mongodb.panache.bugs

import io.quarkus.it.mongodb.panache.person.PersonEntity
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class Bug5885EntityRepository : Bug5885AbstractRepository<PersonEntity>()