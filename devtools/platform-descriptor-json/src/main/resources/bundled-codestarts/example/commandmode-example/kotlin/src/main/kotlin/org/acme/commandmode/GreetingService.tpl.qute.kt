package org.acme.commandmode

import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class GreetingService {
    fun greeting(name: String) = "{greeting.message} $name"
}