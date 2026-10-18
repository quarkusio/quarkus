package io.quarkus.it.mongodb.panache.reactive

import io.quarkus.it.mongodb.panache.BookDTO
import io.quarkus.it.mongodb.panache.book.BookDetail
import io.quarkus.it.mongodb.panache.person.Person
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.mongodb.MongoTestResource
import io.restassured.RestAssured
import io.restassured.RestAssured.get
import io.restassured.parsing.Parser
import io.restassured.response.Response
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.sse.SseEventSource
import java.time.Duration
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.GregorianCalendar
import java.util.concurrent.atomic.LongAdder
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper

@QuarkusTest
@QuarkusTestResource(MongoTestResource::class)
internal open class ReactiveMongodbPanacheResourceTest {
    companion object {
        private val objectMapper = JsonMapper.builder().findAndAddModules().build()
        private val LIST_OF_BOOK_TYPE_REF = object : TypeReference<List<BookDTO>>() {}
        private val LIST_OF_PERSON_TYPE_REF = object : TypeReference<List<Person>>() {}
    }

    @Test
    @Throws(InterruptedException::class)
    fun testReactiveBookEntity() {
        callReactiveBookEndpoint("/reactive/books/entity")
    }

    @Test
    @Throws(InterruptedException::class)
    fun testReactiveBookRepository() {
        callReactiveBookEndpoint("/reactive/books/repository")
    }

    @Test
    fun testReactivePersonEntity() {
        callReactivePersonEndpoint("/reactive/persons/entity")
    }

    @Test
    fun testReactivePersonRepository() {
        callReactivePersonEndpoint("/reactive/persons/repository")
    }

    @Throws(InterruptedException::class)
    private fun callReactiveBookEndpoint(endpoint: String) {
        RestAssured.defaultParser = Parser.JSON

        var list: List<BookDTO> = readList(get(endpoint).asString(), LIST_OF_BOOK_TYPE_REF)
        assertEquals(0, list.size)

        val book1: BookDTO =
            BookDTO()
                .setAuthor("Victor Hugo")
                .setTitle("Les Misérables")
                .setCreationDate(yearToDate(1886))
                .setCategories(listOf("long", "very long"))
                .setDetails(BookDetail().setRating(3).setSummary("A very long book"))
        var response: Response =
            RestAssured.given()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(book1))
                .post(endpoint)
                .andReturn()
        assertEquals(201, response.statusCode())
        Assertions.assertTrue(
            response.header("Location").length > 20
        ) // Assert that id has been populated

        val book2: BookDTO =
            BookDTO()
                .setAuthor("Victor Hugo")
                .setTitle("Notre-Dame de Paris")
                .setCreationDate(yearToDate(1831))
                .setCategories(listOf("long", "quasimodo"))
                .setDetails(BookDetail().setRating(4).setSummary("quasimodo and esmeralda"))
        response =
            RestAssured.given()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(book2))
                .post(endpoint)
                .andReturn()
        assertEquals(201, response.statusCode())

        list = readList(get(endpoint).asString(), LIST_OF_BOOK_TYPE_REF)
        assertEquals(2, list.size)

        val book3: BookDTO =
            BookDTO()
                .setAuthor("Charles Baudelaire")
                .setTitle("Les fleurs du mal")
                .setCreationDate(yearToDate(1857))
                .setCategories(Collections.singletonList("poem"))
                .setDetails(
                    BookDetail().setRating(2).setSummary("Les Fleurs du mal is a volume of poetry.")
                )
        response =
            RestAssured.given()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(book3))
                .post(endpoint)
                .andReturn()
        assertEquals(201, response.statusCode())

        val book4: BookDTO =
            BookDTO()
                .setAuthor("Charles Baudelaire")
                .setTitle("Le Spleen de Paris")
                .setCreationDate(yearToDate(1869))
                .setCategories(Collections.singletonList("poem"))
                .setDetails(
                    BookDetail()
                        .setRating(2)
                        .setSummary("Le Spleen de Paris is a collection of 50 short prose poems.")
                )
        response =
            RestAssured.given()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(book4))
                .patch(endpoint)
                .andReturn()
        assertEquals(202, response.statusCode())

        list = readList(get(endpoint).asString(), LIST_OF_BOOK_TYPE_REF)
        assertEquals(4, list.size)

        // with sort
        list = readList(get("$endpoint?sort=author").asString(), LIST_OF_BOOK_TYPE_REF)
        assertEquals(4, list.size)

        // magic query find("author", author)
        list = readList(get("$endpoint/search/Victor Hugo").asString(), LIST_OF_BOOK_TYPE_REF)
        assertEquals(2, list.size)

        // magic query find("{'author':?1,'title':?1}", author, title)
        var book: BookDTO =
            objectMapper.readValue(
                get("$endpoint/search?author=Victor Hugo&title=Notre-Dame de Paris").asString(),
                BookDTO::class.java,
            )
        assertNotNull(book)

        // date
        book =
            objectMapper.readValue(
                get("$endpoint/search?dateFrom=1885-01-01&dateTo=1887-01-01").asString(),
                BookDTO::class.java,
            )
        assertNotNull(book)

        book =
            objectMapper.readValue(
                get("$endpoint/search2?dateFrom=1885-01-01&dateTo=1887-01-01").asString(),
                BookDTO::class.java,
            )
        assertNotNull(book)

        // magic query find("{'author'::author,'title'::title}", Parameters.with("author",
        // author).and("title", title))
        book =
            objectMapper.readValue(
                get("$endpoint/search2?author=Victor Hugo&title=Notre-Dame de Paris").asString(),
                BookDTO::class.java,
            )
        assertNotNull(book)
        assertNotNull(book.id)
        assertNotNull(book.details)

        // update a book
        book.setTitle("Notre-Dame de Paris 2").setTransientDescription("should not be persisted")
        response =
            RestAssured.given()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(book))
                .put(endpoint)
                .andReturn()
        assertEquals(202, response.statusCode())

        // check that the title has been updated and the transient description ignored
        book =
            objectMapper.readValue(
                get(endpoint + "/" + book.id.toString()).asString(),
                BookDTO::class.java,
            )
        assertEquals("Notre-Dame de Paris 2", book.title)
        Assertions.assertNull(book.transientDescription)

        // delete a book
        response = RestAssured.given().delete(endpoint + "/" + book.id.toString()).andReturn()
        assertEquals(204, response.statusCode())

        list = readList(get(endpoint).asString(), LIST_OF_BOOK_TYPE_REF)
        assertEquals(3, list.size)

        // test some special characters
        list = readList(get("$endpoint/search/Victor'\\ Hugo").asString(), LIST_OF_BOOK_TYPE_REF)
        assertEquals(0, list.size)

        // test SSE : there is no JSON serialization for SSE so the test is not very elegant ...
        val client: Client = ClientBuilder.newClient()
        val target: WebTarget = client.target("http://localhost:8081$endpoint/stream")
        SseEventSource.target(target).build().use { source ->
            val nbEvent = LongAdder()
            source.register { inboundSseEvent ->
                val theBook: BookDTO =
                    objectMapper.readValue(inboundSseEvent.readData(), BookDTO::class.java)
                assertNotNull(theBook)
                nbEvent.increment()
            }
            source.open()
            await().atMost(Duration.ofSeconds(10)).until({ nbEvent.sum() == 3L })
        }

        // delete all
        response = RestAssured.given().delete(endpoint).andReturn()
        assertEquals(204, response.statusCode())
    }

    private fun callReactivePersonEndpoint(endpoint: String) {
        RestAssured.defaultParser = Parser.JSON
        var list: List<Person> = readList(get(endpoint).asString(), LIST_OF_PERSON_TYPE_REF)
        assertEquals(0, list.size)
        val person1 = Person()
        person1.id = 1L
        person1.firstname = "John"
        person1.lastname = "Doe"
        var response: Response =
            RestAssured.given()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(person1))
                .post(endpoint)
                .andReturn()
        assertEquals(201, response.statusCode())
        val person2 = Person()
        person2.id = 2L
        person2.firstname = "Jane"
        person2.lastname = "Doe"
        var person3 = Person()
        person3.id = 3L
        person3.firstname = "Victor"
        person3.lastname = "Hugo"
        val persons = mutableListOf<Person>()
        persons.add(person2)
        persons.add(person3)
        response =
            RestAssured.given()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(persons))
                .post("$endpoint/multiple")
                .andReturn()
        assertEquals(204, response.statusCode())
        val person4 = Person()
        person4.id = 4L
        person4.firstname = "Charles"
        person4.lastname = "Baudelaire"
        response =
            RestAssured.given()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(person4))
                .patch(endpoint)
                .andReturn()
        assertEquals(202, response.statusCode())
        list = readList(get(endpoint).asString(), LIST_OF_PERSON_TYPE_REF)
        assertEquals(4, list.size)

        // with sort
        list = readList(get("$endpoint?sort=firstname").asString(), LIST_OF_PERSON_TYPE_REF)
        assertEquals(4, list.size)

        // with project
        list = readList(get("$endpoint/search/Doe").asString(), LIST_OF_PERSON_TYPE_REF)
        assertEquals(1, list.size)
        assertNotNull(list[0].lastname)
        // expected the firstname field to be null as we project on lastname only
        Assertions.assertNull(list[0].firstname)

        // rename the Doe
        RestAssured.given()
            .queryParam("previousName", "Doe")
            .queryParam("newName", "Dupont")
            .header("Content-Type", "application/json")
            .`when`()
            .post("$endpoint/rename")
            .then()
            .statusCode(200)
        list = readList(get("$endpoint/search/Dupont").asString(), LIST_OF_PERSON_TYPE_REF)
        assertEquals(1, list.size)

        // count
        var count: Long = get("$endpoint/count").asString().toLong()
        assertEquals(4, count)

        // update a person
        person3.lastname = "Webster"
        response =
            RestAssured.given()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(person3))
                .put(endpoint)
                .andReturn()
        assertEquals(202, response.statusCode())

        // check that the title has been updated
        person3 =
            objectMapper.readValue(get("$endpoint/${person3.id}").asString(), Person::class.java)
        assertEquals(3L, person3.id ?: -1L)
        assertEquals("Webster", person3.lastname)

        // delete a person
        response = RestAssured.given().delete(endpoint + "/" + person3.id).andReturn()
        assertEquals(204, response.statusCode())
        count = get("$endpoint/count").asString().toLong()
        assertEquals(3, count)

        // delete all
        response = RestAssured.given().delete(endpoint).andReturn()
        assertEquals(204, response.statusCode())
        count = get("$endpoint/count").asString().toLong()
        assertEquals(0, count)
    }

    private fun yearToDate(year: Int): Date {
        val cal: Calendar = GregorianCalendar()
        cal.set(year, 1, 1)
        return cal.getTime()
    }

    private fun <T> readList(json: String, typeRef: TypeReference<List<T>>): List<T> {
        return objectMapper.readValue(json, typeRef)
    }

    @Test
    fun testMoreEntityFunctionalities() {
        get("/test/reactive/entity").then().statusCode(200)
    }

    @Test
    fun testMoreRepositoryFunctionalities() {
        get("/test/reactive/repository").then().statusCode(200)
    }
}
