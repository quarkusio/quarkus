package io.quarkus.it.mongodb.panache.person

enum class Status(val value: String) {
    DEAD("I'm a Zombie"), ALIVE("I alive!");

    override fun toString(): String {
        return value
    }

}