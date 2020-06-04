package io.quarkus.it.panache

import io.quarkus.it.panache.kotlin.Address
import io.quarkus.it.panache.kotlin.AddressDao
import io.quarkus.it.panache.kotlin.Person
import io.quarkus.it.panache.kotlin.TestEndpoint
import io.quarkus.test.junit.QuarkusTest
import org.hibernate.internal.SessionImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.sql.ResultSet
import javax.inject.Inject
import javax.persistence.EntityManager

@QuarkusTest
class TestEndpointRunner {
    @Inject
    lateinit var endpoint: TestEndpoint

    @Inject
    lateinit var addressDao: AddressDao

    @Inject
    lateinit var em: EntityManager

    @Test
    fun testModel() {
        val con = (em.delegate as SessionImpl).connection()

        val schema = mutableMapOf<String, Map<String, String>>()
        val result: ResultSet = con.metaData.getTables(null, "PUBLIC", null, arrayOf("TABLE"))
        while (result.next()) {
            val tableName: String = result.getString(3)
            val table = mutableMapOf<String, String>()
            schema[tableName] = table
            val columns: ResultSet = con.metaData.getColumns(null, null, tableName, null)
            while (columns.next()) {
                table[columns.getString(4)] = columns.getString(6)
            }
        }
        con.close()

        Assertions.assertEquals("VARCHAR", schema["PERSON2"]?.get("STATUS"), schema.toString())

        endpoint.testModelDao()
        endpoint.testModel()
        endpoint.testBug9036()
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