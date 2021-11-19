package io.quarkus.hibernate.reactive.panache.kotlin.test

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import javax.persistence.Id

class DuplicateIdEntity : PanacheEntity() {
    @Id
    var customId: String? = null
}