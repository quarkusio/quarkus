package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import java.io.Serializable
import java.util.Objects

@Entity
class ObjectWithEmbeddableId : PanacheEntityBase {
    companion object : PanacheCompanionBase<ObjectWithEmbeddableId, ObjectKey>

    @EmbeddedId
    var key: ObjectKey? = null
    var description: String? = null

    @Embeddable
    class ObjectKey : Serializable {
        private var part1: String? = null
        private var part2: String? = null

        constructor() {}
        constructor(part1: String?, part2: String?) {
            this.part1 = part1
            this.part2 = part2
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val objectKey = o as ObjectKey
            return part1 == objectKey.part1 && part2 == objectKey.part2
        }

        override fun hashCode(): Int {
            return Objects.hash(part1, part2)
        }
    }
}
