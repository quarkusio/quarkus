package org.acme

import jakarta.enterprise.context.ApplicationScoped

// ArC can remove `final` from regular intercepted methods, but not from suspend functions,
// so this bean only works if the all-open compiler plugin is applied
@ApplicationScoped
class GreetingService {

    fun greet() = "hello"

    @Logged
    suspend fun greetLater() = greet()
}
