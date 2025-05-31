package io.quarkus.hibernate.reactive.panache.kotlin.deployment.test

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity

@Entity
class MyEntity : PanacheEntity() {
    companion object: PanacheCompanion<MyEntity> {
    }

    lateinit var name: String
}
