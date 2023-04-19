package io.quarkus.it.resteasy.reactive.groovy

import groovy.transform.CompileStatic
import io.quarkus.runtime.annotations.QuarkusMain

import static io.quarkus.runtime.Quarkus.run

@CompileStatic
@QuarkusMain
class GreetingApplication {

    static void main(String[] args) {
        run()
    }
}
