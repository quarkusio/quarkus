package io.quarkus.it.resteasy.reactive.groovy

import groovy.transform.CompileStatic
import jakarta.enterprise.context.RequestScoped

@CompileStatic
@RequestScoped
class UppercaseService {

    def convert(String input) {
        input.toUpperCase(Locale.ROOT)
    }
}
