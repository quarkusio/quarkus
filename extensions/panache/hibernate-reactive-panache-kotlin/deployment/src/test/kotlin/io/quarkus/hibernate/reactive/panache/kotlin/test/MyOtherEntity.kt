package io.quarkus.hibernate.reactive.panache.kotlin.test

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import javax.persistence.Entity

@Entity
class MyOtherEntity : PanacheEntity() {
   companion object : PanacheCompanion<MyOtherEntity>
   var name: String? = null
}