package io.quarkus.it.panache.reactive.kotlin

import jakarta.persistence.Column
import jakarta.persistence.Entity

@Entity
class Bug7721Entity : Bug7721EntitySuperClass() {
    @Column(nullable = false)
    var foo: String = "default"

    init {
        foo = "default" // same as init
        foo = "default" // qualify
        superField = "default"
        superField = "default"
        super.superField = "default"
        val otherEntity = Bug7721OtherEntity()
        otherEntity.foo = "bar" // we want to make sure the setter gets called because it's not our hierarchy
        if (otherEntity.foo != "BAR") throw AssertionError("setter was not called", null)
    }
}
