package io.quarkus.it.mongodb.panache;

import static io.restassured.RestAssured.get;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.quarkus.it.mongodb.panache.book.BookDetail;
import io.quarkus.it.mongodb.panache.person.Person;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;

@QuarkusTest
class BookResourceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookResourceTest.class);
    private static final TypeRef<List<BookDTO>> LIST_OF_BOOK_TYPE_REF = new TypeRef<List<BookDTO>>() {
    };
    private static final TypeRef<List<Person>> LIST_OF_PERSON_TYPE_REF = new TypeRef<List<Person>>() {
    };

    private static MongodExecutable MONGO;

    @BeforeAll
    public static void startMongoDatabase() throws IOException {
        Version.Main version = Version.Main.V4_0;
        int port = 27018;
        LOGGER.info("Starting Mongo {} on port {}", version, port);
        IMongodConfig config = new MongodConfigBuilder()
                .version(version)
                .net(new Net(port, Network.localhostIsIPv6()))
                .build();
        MONGO = MongodStarter.getDefaultInstance().prepare(config);
        MONGO.start();
    }

    @AfterAll
    public static void stopMongoDatabase() {
        if (MONGO != null) {
            MONGO.stop();
        }
    }

    @Test
    public void testBookEntity() {
        callBookEndpoint("/books/entity");
    }

    @Test
    public void testBookRepository() {
        callBookEndpoint("/books/repository");
    }

    @Test
    public void testPersonEntity() {
        callPersonEndpoint("/persons/entity");
    }

    @Test
    public void testPersonRepository() {
        callPersonEndpoint("/persons/repository");
    }

    private void callBookEndpoint(String endpoint) {
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.config
                .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((type, s) -> new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));

        List<BookDTO> list = get(endpoint).as(LIST_OF_BOOK_TYPE_REF);
        Assertions.assertEquals(0, list.size());

        BookDTO book1 = new BookDTO().setAuthor("Victor Hugo").setTitle("Les Misérables")
                .setCreationDate(yearToDate(1886))
                .setCategories(Arrays.asList("long", "very long"))
                .setDetails(new BookDetail().setRating(3).setSummary("A very long book"));
        Response response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(book1)
                .post(endpoint)
                .andReturn();
        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertTrue(response.header("Location").length() > 20);//Assert that id has been populated

        BookDTO book2 = new BookDTO().setAuthor("Victor Hugo").setTitle("Notre-Dame de Paris")
                .setCreationDate(yearToDate(1831))
                .setCategories(Arrays.asList("long", "quasimodo"))
                .setDetails(new BookDetail().setRating(4).setSummary("quasimodo and esmeralda"));
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(book2)
                .post(endpoint)
                .andReturn();
        Assertions.assertEquals(201, response.statusCode());

        list = get(endpoint).as(LIST_OF_BOOK_TYPE_REF);
        Assertions.assertEquals(2, list.size());

        BookDTO book3 = new BookDTO().setAuthor("Charles Baudelaire").setTitle("Les fleurs du mal")
                .setCreationDate(yearToDate(1857))
                .setCategories(Collections.singletonList("poem"))
                .setDetails(new BookDetail().setRating(2).setSummary("Les Fleurs du mal is a volume of poetry."));
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(book3)
                .post(endpoint)
                .andReturn();
        Assertions.assertEquals(201, response.statusCode());

        BookDTO book4 = new BookDTO().setAuthor("Charles Baudelaire").setTitle("Le Spleen de Paris")
                .setCreationDate(yearToDate(1869))
                .setCategories(Collections.singletonList("poem"))
                .setDetails(new BookDetail().setRating(2)
                        .setSummary("Le Spleen de Paris is a collection of 50 short prose poems."));
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(book4)
                .patch(endpoint)
                .andReturn();
        Assertions.assertEquals(202, response.statusCode());

        list = get(endpoint).as(LIST_OF_BOOK_TYPE_REF);
        Assertions.assertEquals(4, list.size());

        //with sort
        list = get(endpoint + "?sort=author").as(LIST_OF_BOOK_TYPE_REF);
        Assertions.assertEquals(4, list.size());

        // magic query find("author", author)
        list = get(endpoint + "/search/Victor Hugo").as(LIST_OF_BOOK_TYPE_REF);
        Assertions.assertEquals(2, list.size());

        // magic query find("{'author':?1,'title':?1}", author, title)
        BookDTO book = get(endpoint + "/search?author=Victor Hugo&title=Notre-Dame de Paris").as(BookDTO.class);
        Assertions.assertNotNull(book);

        // date
        book = get(endpoint + "/search?dateFrom=1885-01-01&dateTo=1887-01-01").as(BookDTO.class);
        Assertions.assertNotNull(book);

        book = get(endpoint + "/search2?dateFrom=1885-01-01&dateTo=1887-01-01").as(BookDTO.class);
        Assertions.assertNotNull(book);

        // magic query find("{'author'::author,'title'::title}", Parameters.with("author", author).and("title", title))
        book = get(endpoint + "/search2?author=Victor Hugo&title=Notre-Dame de Paris").as(BookDTO.class);
        Assertions.assertNotNull(book);
        Assertions.assertNotNull(book.getId());
        Assertions.assertNotNull(book.getDetails());

        //update a book
        book.setTitle("Notre-Dame de Paris 2").setTransientDescription("should not be persisted");
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(book)
                .put(endpoint)
                .andReturn();
        Assertions.assertEquals(202, response.statusCode());

        //check that the title has been updated and the transient description ignored
        book = get(endpoint + "/" + book.getId().toString()).as(BookDTO.class);
        Assertions.assertEquals("Notre-Dame de Paris 2", book.getTitle());
        Assertions.assertNull(book.getTransientDescription());

        //delete a book
        response = RestAssured
                .given()
                .delete(endpoint + "/" + book.getId().toString())
                .andReturn();
        Assertions.assertEquals(204, response.statusCode());

        list = get(endpoint).as(LIST_OF_BOOK_TYPE_REF);
        Assertions.assertEquals(3, list.size());

        //test some special characters
        list = get(endpoint + "/search/Victor'\\ Hugo").as(LIST_OF_BOOK_TYPE_REF);
        Assertions.assertEquals(0, list.size());
    }

    private void callPersonEndpoint(String endpoint) {
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.config
                .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((type, s) -> new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));

        List<Person> list = get(endpoint).as(LIST_OF_PERSON_TYPE_REF);
        Assertions.assertEquals(0, list.size());

        Person person1 = new Person();
        person1.id = 1L;
        person1.firstname = "John";
        person1.lastname = "Doe";
        Response response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(person1)
                .post(endpoint)
                .andReturn();
        Assertions.assertEquals(201, response.statusCode());

        Person person2 = new Person();
        person2.id = 2L;
        person2.firstname = "Jane";
        person2.lastname = "Doe";
        Person person3 = new Person();
        person3.id = 3L;
        person3.firstname = "Victor";
        person3.lastname = "Hugo";
        List<Person> persons = new ArrayList<>();
        persons.add(person2);
        persons.add(person3);
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(persons)
                .post(endpoint + "/multiple")
                .andReturn();
        Assertions.assertEquals(204, response.statusCode());

        Person person4 = new Person();
        person1.id = 4L;
        person1.firstname = "Charles";
        person1.lastname = "Baudelaire";
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(person1)
                .patch(endpoint)
                .andReturn();
        Assertions.assertEquals(202, response.statusCode());

        list = get(endpoint).as(LIST_OF_PERSON_TYPE_REF);
        Assertions.assertEquals(4, list.size());

        //with sort
        list = get(endpoint + "?sort=firstname").as(LIST_OF_PERSON_TYPE_REF);
        Assertions.assertEquals(4, list.size());

        //count
        Long count = get(endpoint + "/count").as(Long.class);
        Assertions.assertEquals(4, count);

        //update a person
        person3.lastname = "Webster";
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(person3)
                .put(endpoint)
                .andReturn();
        Assertions.assertEquals(202, response.statusCode());

        //check that the title has been updated
        person3 = get(endpoint + "/" + person3.id.toString()).as(Person.class);
        Assertions.assertEquals(3L, person3.id);
        Assertions.assertEquals("Webster", person3.lastname);

        //delete a person
        response = RestAssured
                .given()
                .delete(endpoint + "/" + person3.id.toString())
                .andReturn();
        Assertions.assertEquals(204, response.statusCode());

        count = get(endpoint + "/count").as(Long.class);
        Assertions.assertEquals(3, count);

        //delete all
        response = RestAssured
                .given()
                .delete(endpoint)
                .andReturn();
        Assertions.assertEquals(204, response.statusCode());

        count = get(endpoint + "/count").as(Long.class);
        Assertions.assertEquals(0, count);
    }

    private Date yearToDate(int year) {
        Calendar cal = new GregorianCalendar();
        cal.set(year, 1, 1);
        return cal.getTime();
    }

    private Date fromYear(int year) {
        return Date.from(LocalDate.of(year, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
    }

}
