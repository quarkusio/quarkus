package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class DuplicateEntity: PanacheEntityBase {
    companion object : PanacheCompanion<DuplicateEntity>

    @Id
    @GeneratedValue
    var id: Long? = null
}