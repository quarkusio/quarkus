package io.quarkus.it.mongodb.panache.book

import jakarta.enterprise.context.ApplicationScoped

// used only to validate that we can inject CDI beans into Panache repositories written in Kotlin
@ApplicationScoped
class DummyService
