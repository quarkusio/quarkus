package io.quarkus.it.resteasy.reactive.kotlin

import javax.enterprise.context.RequestScoped

@RequestScoped
class RequestScopedKotlinClass {

    var message : String = "initial";

}