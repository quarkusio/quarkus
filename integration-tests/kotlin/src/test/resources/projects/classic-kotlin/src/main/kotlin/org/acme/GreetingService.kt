package org.acme

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class GreetingService {

    fun greet() = "hello"
}