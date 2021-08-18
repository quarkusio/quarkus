package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Bug19420Entity(
        @Id
        var id: String,
        var name: String,
) : PanacheEntityBase {
    companion object : PanacheCompanionBase<Bug19420Entity, String> {
        fun test(): List<Bug19420Entity> = list("name in (?1)", listOf("John", "Kate"))
    }
}