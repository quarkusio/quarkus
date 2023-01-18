package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne

@Entity
class Cat @JvmOverloads constructor(
    var name: String? = null,
    @ManyToOne
    var owner: CatOwner? = null,
    var weight: Double? = null
) : PanacheEntity() {
    companion object : PanacheCompanion<Cat>

    constructor(owner: CatOwner?) : this(null, owner, null)
}
