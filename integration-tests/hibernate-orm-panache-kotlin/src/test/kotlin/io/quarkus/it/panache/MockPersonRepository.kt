package io.quarkus.it.panache

import io.quarkus.it.panache.kotlin.PersonRepository
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Alternative

/**
 * An alternate implementation of [PersonRepository] to demonstrate that Repository entity type can be retrieved
 * even when the class does not directly implement PanacheRepository interface.
 */
@Alternative
@Priority(1)
@ApplicationScoped
open class MockPersonRepository : PersonRepository()
