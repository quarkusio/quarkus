package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepository
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class NamedQueryWith2QueriesRepository : PanacheRepository<NamedQueryWith2QueriesEntity>
