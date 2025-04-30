package io.quarkus.it.panache.kotlin

enum class Status {
    // Make sure to keep the ordinal order different from alphabetical order here
    // See io.quarkus.it.panache.TestEndpointRunner
    LIVING,
    DECEASED
}
