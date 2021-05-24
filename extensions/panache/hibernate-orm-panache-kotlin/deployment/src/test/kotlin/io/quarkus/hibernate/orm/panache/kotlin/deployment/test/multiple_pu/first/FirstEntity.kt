package io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.first

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import javax.persistence.Entity

@Entity
class FirstEntity : PanacheEntity() {
    var name: String? = null
}
