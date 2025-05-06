package org.example

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class SomeBean {

    fun someMethod() = "Hello from SomeBean!"
}