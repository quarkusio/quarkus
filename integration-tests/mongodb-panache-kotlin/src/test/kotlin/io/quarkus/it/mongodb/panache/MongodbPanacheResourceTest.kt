package io.quarkus.it.mongodb.panache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.quarkus.it.mongodb.panache.book.BookDetail
import io.quarkus.it.mongodb.panache.person.Person
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.mongodb.MongoTestResource
import io.restassured.RestAssured
import io.restassured.RestAssured.get
import io.restassured.common.mapper.TypeRef
import io.restassured.config.ObjectMapperConfig
import io.restassured.parsing.Parser
import io.restassured.response.Response
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.GregorianCalendar

@QuarkusTest
@QuarkusTestResource(MongoTestResource::class)
@DisabledOnOs(OS.WINDOWS)
open class MongodbPanacheResourceTest {
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
        RestAssured.config
                .objectMapperConfig(ObjectMapperConfig().jackson2ObjectMapperFactory { _, _ ->
                    ObjectMapper()
                            .registerModule(Jdk8Module())
                            .registerModule(JavaTimeModule())
                            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                })
        var list: List<BookDTO> = get(endpoint).`as`(LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(0, list.size)
        val book1: BookDTO = BookDTO().setAuthor("Victor Hugo").setTitle("Les Misérables")
                .setCreationDate(yearToDate(1886))
                .setCategories(listOf("long", "very long"))
                .setDetails(BookDetail().setRating(3).setSummary("A very long book"))
        var response: Response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(book1)
                .post(endpoint)
                .andReturn()
        Assertions.assertEquals(201, response.statusCode())
        Assertions.assertTrue(response.header("Location").length > 20) //Assert that id has been populated
        val book2: BookDTO = BookDTO().setAuthor("Victor Hugo").setTitle("Notre-Dame de Paris")
                .setCreationDate(yearToDate(1831))
                .setCategories(listOf("long", "quasimodo"))
                .setDetails(BookDetail().setRating(4).setSummary("quasimodo and esmeralda"))
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(book2)
                .post(endpoint)
                .andReturn()
        Assertions.assertEquals(201, response.statusCode())
        list = get(endpoint).`as`(LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(2, list.size)
        val book3: BookDTO = BookDTO().setAuthor("Charles Baudelaire").setTitle("Les fleurs du mal")
                .setCreationDate(yearToDate(1857))
                .setCategories(Collections.singletonList("poem"))
                .setDetails(BookDetail().setRating(2).setSummary("Les Fleurs du mal is a volume of poetry."))
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(book3)
                .post(endpoint)
                .andReturn()
        Assertions.assertEquals(201, response.statusCode())
        val book4: BookDTO = BookDTO().setAuthor("Charles Baudelaire").setTitle("Le Spleen de Paris")
                .setCreationDate(yearToDate(1869))
                .setCategories(Collections.singletonList("poem"))
                .setDetails(BookDetail().setRating(2)
                        .setSummary("Le Spleen de Paris is a collection of 50 short prose poems."))
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(book4)
                .patch(endpoint)
                .andReturn()
        Assertions.assertEquals(202, response.statusCode())
        list = get(endpoint).`as`(LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(4, list.size)

        //with sort
        list = get("$endpoint?sort=author").`as`(LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(4, list.size)

        // magic query find("author", author)
        list = get("$endpoint/search/Victor Hugo").`as`(LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(2, list.size)
        // we have a projection so we should not have the details field but we should have the title thanks to @BsonProperty
        Assertions.assertNotNull(list[0].title)
        Assertions.assertNull(list[0].details)

        // magic query find("{'author':?1,'title':?1}", author, title)
        var book: BookDTO = get("$endpoint/search?author=Victor Hugo&title=Notre-Dame de Paris").`as`(BookDTO::class.java)
        Assertions.assertNotNull(book)

        // date
        book = get("$endpoint/search?dateFrom=1885-01-01&dateTo=1887-01-01").`as`(BookDTO::class.java)
        Assertions.assertNotNull(book)
        book = get("$endpoint/search2?dateFrom=1885-01-01&dateTo=1887-01-01").`as`(BookDTO::class.java)
        Assertions.assertNotNull(book)

        // magic query find("{'author'::author,'title'::title}", Parameters.with("author", author).and("title", title))
        book = get("$endpoint/search2?author=Victor Hugo&title=Notre-Dame de Paris").`as`(BookDTO::class.java)
        Assertions.assertNotNull(book)
        Assertions.assertNotNull(book.id)
        Assertions.assertNotNull(book.details)

        //update a book
        book.setTitle("Notre-Dame de Paris 2").setTransientDescription("should not be persisted")
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(book)
                .put(endpoint)
                .andReturn()
        Assertions.assertEquals(202, response.statusCode())

        //check that the title has been updated and the transient description ignored
        book = get(endpoint + "/" + book.id.toString()).`as`(BookDTO::class.java)
        Assertions.assertNotNull(book)
        Assertions.assertEquals("Notre-Dame de Paris 2", book.title)
        Assertions.assertNull(book.transientDescription)

        //delete a book
        response = RestAssured
                .given()
                .delete(endpoint + "/" + book.id.toString())
                .andReturn()
        Assertions.assertEquals(204, response.statusCode())
        list = get(endpoint).`as`(LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(3, list.size)

        //test some special characters
        list = get("$endpoint/search/Victor'\\ Hugo").`as`(LIST_OF_BOOK_TYPE_REF)
        Assertions.assertEquals(0, list.size)

        //delete all
        response = RestAssured
                .given()
                .delete(endpoint)
                .andReturn()
        Assertions.assertEquals(204, response.statusCode())
    }

    private fun callPersonEndpoint(endpoint: String) {
        RestAssured.defaultParser = Parser.JSON
        RestAssured.config
                .objectMapperConfig(ObjectMapperConfig().jackson2ObjectMapperFactory { _, _ ->
                    ObjectMapper()
                            .registerModule(Jdk8Module())
                            .registerModule(JavaTimeModule())
                            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                })
        var list: List<Person> = get(endpoint).`as`(LIST_OF_PERSON_TYPE_REF)
        Assertions.assertEquals(0, list.size)
        val person1 = Person()
        person1.id = 1L
        person1.firstname = "John"
        person1.lastname = "Doe"
        var response: Response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(person1)
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
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(persons)
                .post("$endpoint/multiple")
                .andReturn()
        Assertions.assertEquals(204, response.statusCode())
        val person4 = Person()
        person4.id = 4L
        person4.firstname = "Charles"
        person4.lastname = "Baudelaire"
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(person4)
                .patch(endpoint)
                .andReturn()
        Assertions.assertEquals(202, response.statusCode())
        list = get(endpoint).`as`(LIST_OF_PERSON_TYPE_REF)
        Assertions.assertEquals(4, list.size)

        //with sort
        list = get("$endpoint?sort=firstname").`as`(LIST_OF_PERSON_TYPE_REF)
        Assertions.assertEquals(4, list.size)

        //with project
        list = get("$endpoint/search/Doe").`as`(LIST_OF_PERSON_TYPE_REF)
        Assertions.assertEquals(1, list.size)
        Assertions.assertNotNull(list[0].lastname)
        //expected the firstname field to be null as we project on lastname only
        Assertions.assertNull(list[0].firstname)

        //rename the Doe
        RestAssured
                .given()
                .queryParam("previousName", "Doe").queryParam("newName", "Dupont")
                .header("Content-Type", "application/json")
                .`when`().post("$endpoint/rename")
                .then().statusCode(200)
        list = get("$endpoint/search/Dupont").`as`(LIST_OF_PERSON_TYPE_REF)
        Assertions.assertEquals(1, list.size)

        //count
        var count: Long = get("$endpoint/count").`as`(Long::class.java)
        Assertions.assertEquals(4, count)

        //update a person
        person3.lastname = "Webster"
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(person3)
                .put(endpoint)
                .andReturn()
        Assertions.assertEquals(202, response.statusCode())

        //check that the title has been updated
        person3 = get("$endpoint/${person3.id}").`as`(Person::class.java)
        Assertions.assertEquals(3L, person3.id ?: -1)
        Assertions.assertEquals("Webster", person3.lastname)

        //delete a person
        response = RestAssured
                .given()
                .delete("$endpoint/${person3.id}")
                .andReturn()
        Assertions.assertEquals(204, response.statusCode())
        count = get("$endpoint/count").`as`(Long::class.java)
        Assertions.assertEquals(3, count)

        //delete all
        response = RestAssured
                .given()
                .delete(endpoint)
                .andReturn()
        Assertions.assertEquals(204, response.statusCode())
        count = get("$endpoint/count").`as`(Long::class.java)
        Assertions.assertEquals(0, count)
    }

    private fun yearToDate(year: Int): Date {
        val cal: Calendar = GregorianCalendar()
        cal.set(year, 1, 1)
        return cal.time
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

    companion object {
        private val LIST_OF_BOOK_TYPE_REF: TypeRef<List<BookDTO>> = object : TypeRef<List<BookDTO>>() {}
        private val LIST_OF_PERSON_TYPE_REF: TypeRef<List<Person>> = object : TypeRef<List<Person>>() {}
    }
}