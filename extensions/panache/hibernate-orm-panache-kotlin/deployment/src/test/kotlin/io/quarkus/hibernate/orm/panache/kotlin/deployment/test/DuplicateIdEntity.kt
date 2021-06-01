package io.quarkus.hibernate.orm.panache.kotlin.deployment.test

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import javax.persistence.Id

class DuplicateIdEntity : PanacheEntity() {
    @Id
    var customId: String? = null
}
