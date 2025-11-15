package io.quarkus.it.panache

import io.quarkus.it.panache.kotlin.Address
import io.quarkus.it.panache.kotlin.AddressDao
import io.quarkus.it.panache.kotlin.Person
import io.quarkus.it.panache.kotlin.TestEndpoint
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import java.sql.ResultSet
import org.hibernate.engine.spi.SessionImplementor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

@QuarkusTest
class TestEndpointRunner {
    @Inject lateinit var endpoint: TestEndpoint

    @Inject lateinit var addressDao: AddressDao

    @Inject lateinit var em: EntityManager

    @Test
    fun testModel() {
        val con =
            (em.delegate as SessionImplementor).jdbcCoordinator.logicalConnection.physicalConnection

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

        Assertions.assertEquals(
            // The H2 dialect uses native enums by default for @Enumerated(EnumType.STRING)
            // We're also checking enum value order here:
            // with @Enumerated(EnumType.STRING), enum values should be in alphabetical order
            // (which is different from the ordinal order)
            "ENUM('DECEASED', 'LIVING')",
            schema["PERSON2"]?.get("STATUS"),
            schema.toString(),
        )

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
        } catch (ignored: java.lang.UnsupportedOperationException) {
            fail("This method should have been replaced", ignored)
        }
    }
}
