package io.quarkus.it.rest

import kotlinx.serialization.Serializable

@Serializable
data class Country(
    var name: String,
    var alpha2Code: String,
    var capital: String,
    var currencies: List<Currency>,
) {
    override fun toString(): String {
        TODO("serialization should never call this method")
    }
}

@Serializable
data class Currency(var code: String, var name: String, var symbol: String) {
    override fun toString(): String {
        TODO("serialization should never call this method")
    }
}
