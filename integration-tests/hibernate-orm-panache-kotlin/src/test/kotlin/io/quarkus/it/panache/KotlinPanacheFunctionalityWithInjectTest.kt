package io.quarkus.it.panache

import io.quarkus.it.panache.kotlin.Person
import io.quarkus.it.panache.kotlin.PersonRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Test Panache operations running in Quarkus using injected resources
 */
@QuarkusTest
open class KotlinPanacheFunctionalityWithInjectTest {
    @Inject
    lateinit var personDao: PersonRepository

    @Test
    @Transactional
    open fun testPanacheInTest() {
        Assertions.assertTrue(personDao.findAll().list().isEmpty())
        Assertions.assertTrue(Person.findAll().list().isEmpty())
    }
}
