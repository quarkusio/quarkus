package io.quarkus.it.resteasy.reactive.groovy

import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.enterprise.context.RequestScoped

@RegisterForReflection
@RequestScoped
class RequestScopedGroovyClass {

    String message = "initial"
}
