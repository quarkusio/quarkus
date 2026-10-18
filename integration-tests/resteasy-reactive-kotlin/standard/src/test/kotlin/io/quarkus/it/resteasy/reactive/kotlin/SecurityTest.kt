package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.vertx.core.http.HttpHeaders
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test

@QuarkusTest
class SecurityTest {

    @Test
    fun testAuthorizationPolicyOnSuspendedMethod_MethodLevel() {
        When { get("/secured-method/authorization-policy-suspend") } Then { statusCode(403) }
        Given { header(HttpHeaders.AUTHORIZATION.toString(), "you-can-trust-me") } When
            {
                get("/secured-method/authorization-policy-suspend")
            } Then
            {
                statusCode(200)
                body(CoreMatchers.`is`("Hello from Quarkus REST"))
            }
    }

    @Test
    fun testAuthorizationPolicyOnSuspendedMethod_ClassLevel() {
        // test class-level annotation is applied on a secured method
        When { get("/secured-class/authorization-policy-suspend") } Then { statusCode(403) }
        Given { header(HttpHeaders.AUTHORIZATION.toString(), "you-can-trust-me") } When
            {
                get("/secured-class/authorization-policy-suspend")
            } Then
            {
                statusCode(200)
                body(CoreMatchers.`is`("Hello from Quarkus REST"))
            }

        // test method-level @PermitAll has priority over @AuthorizationPolicy on the class
        When { get("/secured-class/public") } Then
            {
                statusCode(200)
                body(CoreMatchers.`is`("Hello to everyone!"))
            }
    }
}
