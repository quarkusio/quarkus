package io.quarkus.hibernate.reactive.panache.kotlin.test

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import javax.persistence.Entity

@Entity
class MyTestEntity : PanacheEntity() {
   companion object : PanacheCompanion<MyTestEntity>
   var name: String? = null
}