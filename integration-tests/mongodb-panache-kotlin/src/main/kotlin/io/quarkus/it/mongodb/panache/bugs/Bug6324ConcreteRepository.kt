package io.quarkus.it.mongodb.panache.bugs

import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class Bug6324ConcreteRepository : Bug6324AbstractRepository<NeedReflection>()