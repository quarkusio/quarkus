package io.quarkus.it.kotser

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType.JSON
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.MediaType
import java.util.Properties
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
open class ResourceTest {

    val nameField: String
    var defaulted = "defaulted"

    init {
        val properties = Properties()
        properties.load(javaClass.getResourceAsStream("/application.properties"))
        val strategy: String? =
            properties.get("quarkus.kotlin-serialization.json.naming-strategy") as String?
        when (strategy) {
            "JsonNamingStrategy.SnakeCase" -> nameField = "full_name"
            TitleCase::class.qualifiedName -> {
                nameField = "FullName"
                defaulted = "Defaulted"
            }
            null -> nameField = "fullName"
            else -> throw IllegalArgumentException("unknown strategy: $strategy")
        }
    }

    @Test
    fun testGetFlow() {
        When { get("/flow") } Then
            {
                statusCode(200)
                body(`is`("""[{"$nameField":"Jim Halpert","$defaulted":"hi there!"}]"""))
            }
    }

    @Test
    fun testGet() {
        When { get("/") } Then
            {
                statusCode(200)
                body(`is`("""{"$nameField":"Jim Halpert","$defaulted":"hi there!"}"""))
            }
    }

    @Test
    fun testRestResponse() {
        When { get("/restResponse") } Then
            {
                statusCode(200)
                body(`is`("""{"$nameField":"Jim Halpert","$defaulted":"hi there!"}"""))
            }
    }

    @Test
    fun testRestResponseList() {
        When { get("/restResponseList") } Then
            {
                statusCode(200)
                body(`is`("""[{"$nameField":"Jim Halpert","$defaulted":"hi there!"}]"""))
            }
    }

    @Test
    fun testGetUnknownType() {
        When { get("/unknownType") } Then
            {
                statusCode(200)
                body(`is`("""{"$nameField":"Foo Bar","$defaulted":"hey"}"""))
            }
    }

    @Test
    fun testSuspendGet() {
        When { get("/suspend") } Then
            {
                statusCode(200)
                body(`is`("""{"$nameField":"Jim Halpert","$defaulted":"hi there!"}"""))
            }
    }

    @Test
    fun testSuspendGetList() {
        When { get("/suspendList") } Then
            {
                statusCode(200)
                body(`is`("""[{"$nameField":"Jim Halpert","$defaulted":"hi there!"}]"""))
            }
    }

    @Test
    fun testPost() {
        Given {
            body("""{ "$nameField": "Pam Beasley" }""")
            contentType(JSON)
        } When
            {
                post("/")
            } Then
            {
                statusCode(200)
                body(`is`("""{"$nameField":"Pam Halpert","$defaulted":"hi there!"}"""))
            }
    }

    @Test
    fun testCreateAndFetch() {
        Given { log().ifValidationFails() } When
            {
                accept(MediaType.TEXT_PLAIN)
                get("/create")
            } Then
            {
                log().ifValidationFails()
                statusCode(200)
                body(CoreMatchers.equalTo("hello, world"))
            }
    }

    @Test
    fun testEmptyList() {
        When { get("/emptyList") } Then { statusCode(200) }
    }

    @Test
    fun testEmptyMap() {
        When { get("/emptyList") } Then { statusCode(200) }
    }

    @Test
    fun testEmptySet() {
        When { get("/emptySet") } Then { statusCode(200) }
    }
}
