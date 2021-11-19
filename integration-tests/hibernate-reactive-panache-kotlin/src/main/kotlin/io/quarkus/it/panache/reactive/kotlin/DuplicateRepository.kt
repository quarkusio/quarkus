package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepositoryBase
import io.smallrye.mutiny.Uni

class DuplicateRepository : PanacheRepositoryBase<DuplicateEntity, Int> {
    override fun findById(id: Int): Uni<DuplicateEntity> {
        return super.findById(id)
    }
}