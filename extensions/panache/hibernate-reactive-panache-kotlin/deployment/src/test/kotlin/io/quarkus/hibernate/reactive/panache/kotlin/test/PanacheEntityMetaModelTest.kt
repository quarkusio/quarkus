package io.quarkus.hibernate.reactive.panache.kotlin.test

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity_
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PanacheEntityMetaModelTest {
    @Test
    fun testMetaModelExistence() {
        Assertions.assertEquals("id", PanacheEntity_.ID)
    }
}