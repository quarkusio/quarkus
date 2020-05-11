package io.quarkus.hibernate.orm.panache.kotlin

import com.fasterxml.jackson.annotation.JsonIgnore
import io.quarkus.hibernate.orm.panache.kotlin.runtime.JpaOperations
import javax.json.bind.annotation.JsonbTransient

interface PanacheEntityBase {
    @JsonbTransient
    @JsonIgnore
    fun isPersistent() = JpaOperations.isPersistent(this)

    fun persist() = JpaOperations.persist(this)

    fun persistAndFlush() {
        JpaOperations.persist(this)
        JpaOperations.flush()
    }

    fun delete() = JpaOperations.delete(this)

    fun flush() = JpaOperations.flush()
}
