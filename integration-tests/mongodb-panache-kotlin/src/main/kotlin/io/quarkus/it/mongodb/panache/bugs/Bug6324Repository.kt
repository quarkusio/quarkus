package io.quarkus.it.mongodb.panache.bugs

import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class Bug6324Repository : PanacheMongoRepository<NeedReflection>
