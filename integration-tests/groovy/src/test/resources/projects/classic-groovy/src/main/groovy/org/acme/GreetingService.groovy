package org.acme

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class GreetingService {

    def greet() { 'hello' }
}