package io.quarkus.it.resteasy.reactive.groovy

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
class Country {
    String name, capital
    Country() {}
    Country(name, capital) {
        this.name = name
        this.capital = capital
    }
}
