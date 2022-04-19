package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import java.util.Locale
import javax.persistence.Entity

@Entity
class Bug7721OtherEntity : PanacheEntity() {
    var foo: String? = null
        set(value) {
            field = value?.uppercase(Locale.getDefault())
        }
}
