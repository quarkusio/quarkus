package io.quarkus.hibernate.orm.panache.kotlin.deployment.test

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity

@Entity
class MyEntity : PanacheEntity() {
    companion object: PanacheCompanion<MyEntity> {
    }

    lateinit var name: String
}
