package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepositoryBase
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
open class DogDao : PanacheRepositoryBase<Dog, Int>