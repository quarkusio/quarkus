package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity
import java.util.Locale

@Entity
class Bug7721OtherEntity : PanacheEntity() {
    var foo: String? = null
        set(value) {
            field = value?.uppercase(Locale.getDefault())
        }
}
