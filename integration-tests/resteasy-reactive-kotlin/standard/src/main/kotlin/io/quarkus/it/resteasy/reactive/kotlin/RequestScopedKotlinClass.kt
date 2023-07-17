package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.enterprise.context.RequestScoped

@RequestScoped
class RequestScopedKotlinClass {

    var message: String = "initial"
}
