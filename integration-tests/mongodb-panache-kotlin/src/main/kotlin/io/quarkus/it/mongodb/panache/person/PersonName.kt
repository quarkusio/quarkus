package io.quarkus.it.mongodb.panache.person

class PersonName {
    var lastname: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PersonName) return false

        if (lastname != other.lastname) return false

        return true
    }

    override fun hashCode(): Int {
        return lastname?.hashCode() ?: 0
    }
}
