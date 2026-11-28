package org.acme

import jakarta.enterprise.context.ApplicationScoped

// open only through the all-open compiler plugin, intercepted suspend functions must not be final
@ApplicationScoped
class GreetingService {

    @Logged
    suspend fun greet(): String = "hello"
}
