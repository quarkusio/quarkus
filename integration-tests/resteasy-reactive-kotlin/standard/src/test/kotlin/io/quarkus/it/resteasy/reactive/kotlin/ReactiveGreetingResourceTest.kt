package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class ReactiveGreetingResourceTest {

    @Test
    fun testResource() {
        When { get("/test.txt") } Then { statusCode(200) }
    }

    @Test
    fun testHello() {
        When { get("/hello-resteasy-reactive/") } Then
            {
                statusCode(200)
                body(
                    `is`("Hello Quarkus REST")
                ) // the result comes from EntityStreamSettingContainerResponseFilter
            }
    }

    @Test
    fun testStandard() {
        When { get("/hello-resteasy-reactive/standard") } Then
            {
                statusCode(200)
                body(`is`("Hello RestEASY Reactive"))
            }
    }

    @Test
    fun testNamedHello() {
        When { get("/hello-resteasy-reactive/Bob") } Then
            {
                statusCode(200)
                body(`is`("Hello Bob"))
            }
    }
}
