package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import java.io.Serializable

@Entity
@IdClass(ObjectWithCompositeId.ObjectKey::class)
class ObjectWithCompositeId : PanacheEntityBase {
    companion object : PanacheCompanionBase<ObjectWithCompositeId, ObjectKey>

    @Id
    var part1: String? = null

    @Id
    var part2: String? = null
    var description: String? = null

    internal class ObjectKey : Serializable {
        private var part1: String? = null
        private var part2: String? = null

        constructor() {}
        constructor(part1: String?, part2: String?) {
            this.part1 = part1
            this.part2 = part2
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ObjectKey) return false

            if (part1 != other.part1) return false
            if (part2 != other.part2) return false

            return true
        }

        override fun hashCode(): Int {
            var result = part1?.hashCode() ?: 0
            result = 31 * result + (part2?.hashCode() ?: 0)
            return result
        }
    }
}
