package io.quarkus.it.panache

import io.quarkus.it.panache.kotlin.PersonRepository
import javax.annotation.Priority
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Alternative

/**
 * An alternate implementation of [PersonRepository] to demonstrate that Repository entity type can be retrieved
 * even when the class does not directly implement PanacheRepository interface.
 */
@Alternative
@Priority(1)
@ApplicationScoped
open class MockPersonRepository : PersonRepository()