package io.quarkus.it.mongodb.panache

import io.quarkus.it.mongodb.panache.book.BookDetail
import io.quarkus.it.mongodb.panache.person.Person
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.mongodb.MongoTestResource
import io.restassured.RestAssured
import io.restassured.RestAssured.get
import io.restassured.parsing.Parser
import io.restassured.response.Response
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.GregorianCalendar
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper

@QuarkusTest
@QuarkusTestResource(MongoTestResource::class)
open class MongodbPanacheResourceTest {

    companion object {
        private val objectMapper = JsonMapper.builder().findAndAddModules().build()
        private val LIST_OF_BOOK_TYPE_REF = object : TypeReference<List<BookDTO>>() {}
        private val LIST_OF_PERSON_TYPE_REF = object : TypeReference<List<Person>>() {}
    }

    @Test
    fun testBookEntity() {
        callBookEndpoint("/books/entity")
    }

    @Test
    fun testBookRepository() {
        callBookEndpoint("/books/repository")
    }

    @Test
    fun testPersonEntity() {
        callPersonEndpoint("/persons/entity")
    }

    @Test
    fun testPersonRepository() {
        callPersonEndpoint("/persons/repository")
    }

    private fun callBookEndpoint(endpoint: String) {
        RestAssured.defaultParser = Parser.JSON
        var list: List<BookDTO> = readList(get(endpoint).asString(), LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(0, list.size)
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
        Assertions.assertEquals(201, response.statusCode())
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
        Assertions.assertEquals(201, response.statusCode())
        list = readList(get(endpoint).asString(), LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(2, list.size)
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
        Assertions.assertEquals(201, response.statusCode())
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
        Assertions.assertEquals(202, response.statusCode())
        list = readList(get(endpoint).asString(), LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(4, list.size)

        // with sort
        list = readList(get("$endpoint?sort=author").asString(), LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(4, list.size)

        // magic query find("author", author)
        list = readList(get("$endpoint/search/Victor Hugo").asString(), LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(2, list.size)
        // we have a projection so we should not have the details field but we should have the title
        // thanks to @BsonProperty
        Assertions.assertNotNull(list[0].title)
        Assertions.assertNull(list[0].details)

        // magic query find("{'author':?1,'title':?1}", author, title)
        var book: BookDTO =
            objectMapper.readValue(
                get("$endpoint/search?author=Victor Hugo&title=Notre-Dame de Paris").asString(),
                BookDTO::class.java,
            )
        Assertions.assertNotNull(book)

        // date
        book =
            objectMapper.readValue(
                get("$endpoint/search?dateFrom=1885-01-01&dateTo=1887-01-01").asString(),
                BookDTO::class.java,
            )
        Assertions.assertNotNull(book)
        book =
            objectMapper.readValue(
                get("$endpoint/search2?dateFrom=1885-01-01&dateTo=1887-01-01").asString(),
                BookDTO::class.java,
            )
        Assertions.assertNotNull(book)

        // magic query find("{'author'::author,'title'::title}", Parameters.with("author",
        // author).and("title", title))
        book =
            objectMapper.readValue(
                get("$endpoint/search2?author=Victor Hugo&title=Notre-Dame de Paris").asString(),
                BookDTO::class.java,
            )
        Assertions.assertNotNull(book)
        Assertions.assertNotNull(book.id)
        Assertions.assertNotNull(book.details)

        // update a book
        book.setTitle("Notre-Dame de Paris 2").setTransientDescription("should not be persisted")
        response =
            RestAssured.given()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(book))
                .put(endpoint)
                .andReturn()
        Assertions.assertEquals(202, response.statusCode())

        // check that the title has been updated and the transient description ignored
        book =
            objectMapper.readValue(
                get(endpoint + "/" + book.id.toString()).asString(),
                BookDTO::class.java,
            )
        Assertions.assertNotNull(book)
        Assertions.assertEquals("Notre-Dame de Paris 2", book.title)
        Assertions.assertNull(book.transientDescription)

        // delete a book
        response = RestAssured.given().delete(endpoint + "/" + book.id.toString()).andReturn()
        Assertions.assertEquals(204, response.statusCode())
        list = readList(get(endpoint).asString(), LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(3, list.size)

        // test some special characters
        list = readList(get("$endpoint/search/Victor'\\ Hugo").asString(), LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(0, list.size)

        // delete all
        response = RestAssured.given().delete(endpoint).andReturn()
        Assertions.assertEquals(204, response.statusCode())
    }

    private fun callPersonEndpoint(endpoint: String) {
        RestAssured.defaultParser = Parser.JSON
        var list: List<Person> = readList(get(endpoint).asString(), LIST_OF_PERSON_TYPE_REF)
        Assertions.assertEquals(0, list.size)
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
        Assertions.assertEquals(201, response.statusCode())
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
        Assertions.assertEquals(204, response.statusCode())
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
        Assertions.assertEquals(202, response.statusCode())
        list = readList(get(endpoint).asString(), LIST_OF_PERSON_TYPE_REF)
        Assertions.assertEquals(4, list.size)

        // with sort
        list = readList(get("$endpoint?sort=firstname").asString(), LIST_OF_PERSON_TYPE_REF)
        Assertions.assertEquals(4, list.size)

        // with project
        list = readList(get("$endpoint/search/Doe").asString(), LIST_OF_PERSON_TYPE_REF)
        Assertions.assertEquals(1, list.size)
        Assertions.assertNotNull(list[0].lastname)
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
        Assertions.assertEquals(1, list.size)

        // count
        var count: Long = get("$endpoint/count").asString().toLong()
        Assertions.assertEquals(4, count)

        // update a person
        person3.lastname = "Webster"
        response =
            RestAssured.given()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(person3))
                .put(endpoint)
                .andReturn()
        Assertions.assertEquals(202, response.statusCode())

        // check that the title has been updated
        person3 =
            objectMapper.readValue(get("$endpoint/${person3.id}").asString(), Person::class.java)
        Assertions.assertEquals(3L, person3.id ?: -1)
        Assertions.assertEquals("Webster", person3.lastname)

        // delete a person
        response = RestAssured.given().delete("$endpoint/${person3.id}").andReturn()
        Assertions.assertEquals(204, response.statusCode())
        count = get("$endpoint/count").asString().toLong()
        Assertions.assertEquals(3, count)

        // delete all
        response = RestAssured.given().delete(endpoint).andReturn()
        Assertions.assertEquals(204, response.statusCode())
        count = get("$endpoint/count").asString().toLong()
        Assertions.assertEquals(0, count)
    }

    private fun yearToDate(year: Int): Date {
        val cal: Calendar = GregorianCalendar()
        cal.set(year, 1, 1)
        return cal.time
    }

    private fun <T> readList(json: String, typeRef: TypeReference<List<T>>): List<T> {
        return objectMapper.readValue(json, typeRef)
    }

    @Test
    fun testBug5274() {
        get("/bugs/5274").then().body(`is`("OK"))
    }

    @Test
    fun testBug5885() {
        get("/bugs/5885").then().body(`is`("OK"))
    }

    @Test
    fun testDatesFormat() {
        get("/bugs/dates").then().statusCode(200)
    }

    @Test
    fun testNeedReflection() {
        get("/bugs/6324").then().statusCode(200)
        get("/bugs/6324/abstract").then().statusCode(200)
    }

    @Test
    fun testBug7415() {
        get("/bugs/7415").then().statusCode(200)
    }

    @Test
    fun testMoreEntityFunctionalities() {
        get("/test/imperative/entity").then().statusCode(200)
    }

    @Test
    fun testMoreRepositoryFunctionalities() {
        get("/test/imperative/repository").then().statusCode(200)
    }
}
