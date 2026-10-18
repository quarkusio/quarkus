package io.quarkus.logging

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity

@Entity
open class LoggingEntity : PanacheEntity() {
    fun test() {
        Log.info("Hello from entity")
    }
}
