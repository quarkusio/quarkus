package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.enterprise.context.RequestScoped
import java.util.Locale

@RequestScoped
class UppercaseService {

    fun convert(input: String) = input.uppercase(Locale.ROOT)
}
