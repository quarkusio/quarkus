package io.quarkus.it.panache

import io.quarkus.it.panache.kotlin.Address
import io.quarkus.it.panache.kotlin.AddressDao
import io.quarkus.it.panache.kotlin.Person
import io.quarkus.it.panache.kotlin.TestEndpoint
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import javax.inject.Inject

@QuarkusTest
class TestEndpointRunner {
    @Inject
    lateinit var endpoint: TestEndpoint

    @Inject
    lateinit var addressDao: AddressDao

    @Test
    fun testModel() {
        endpoint.testModelDao()
        endpoint.testModel()
        endpoint.testAccessors()
        endpoint.testModel1()
        endpoint.testModel2()
        endpoint.testModel3()
    }

    @Test
    fun overrides() {
        invokeOverload { Address.count("", mapOf()) }
        invokeOverload { Person.findOrdered() }
        invokeOverload { addressDao.count("", mapOf()) }

        invokeNonOverload { Address.count() }
        invokeNonOverload { addressDao.count() }

        invokeNonOverload { addressDao.findById(12) }
        invokeNonOverload { Address.findById(12) }
    }

    private fun invokeOverload(function: () -> Any?) {
        try {
            function()
            fail("The override explicitly throws an exception and quarkus overwrote it.")
        } catch (ignored: UnsupportedOperationException) {
            // all good
        }
    }

    private fun invokeNonOverload(function: () -> Any?) {
        try {
            function()
        } catch (ignored: Exception) {
            fail("This method should have been replaced", ignored)
        }
    }
}