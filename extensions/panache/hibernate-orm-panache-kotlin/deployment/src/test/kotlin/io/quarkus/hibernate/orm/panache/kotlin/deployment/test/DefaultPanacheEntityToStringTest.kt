package io.quarkus.hibernate.orm.panache.kotlin.deployment.test

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DefaultPanacheEntityToStringTest {
    @Test
    fun testDefaultToStringMethod() {
        val myPanacheEntityWithId = MyPanacheEntity(2912L)
        Assertions.assertEquals("MyPanacheEntity<2912>", myPanacheEntityWithId.toString())
        val myPanacheEntityWithNullId = MyPanacheEntity(null)
        Assertions.assertEquals("MyPanacheEntity<null>", myPanacheEntityWithNullId.toString())
    }

    internal class MyPanacheEntity(id: Long?) : PanacheEntity() {
        init {
            this.id = id
        }
    }
}
