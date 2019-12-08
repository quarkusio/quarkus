package org.acme

import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class GreetingService {

    fun greet() = "hello"
}