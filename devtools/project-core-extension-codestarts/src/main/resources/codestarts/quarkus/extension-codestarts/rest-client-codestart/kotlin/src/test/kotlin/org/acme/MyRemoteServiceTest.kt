package org.acme

import io.quarkus.test.junit.QuarkusTest
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import javax.inject.Inject

@QuarkusTest
class MyRemoteServiceTest {

    @Inject
    @RestClient
    lateinit var myRemoteService: MyRemoteService

    @Test
    fun testRestClientEndpoint() {
        val restClientExtensions = myRemoteService.getExtensionsById("io.quarkus:quarkus-rest-client")
        Assertions.assertEquals(1, restClientExtensions.size)
        restClientExtensions.forEach {
            Assertions.assertEquals("io.quarkus:quarkus-rest-client", it.id)
            Assertions.assertEquals("REST Client Classic", it.name)
            Assertions.assertTrue(it.keywords.size > 1)
            Assertions.assertTrue(it.keywords.contains("rest-client"))
        }
    }
}
