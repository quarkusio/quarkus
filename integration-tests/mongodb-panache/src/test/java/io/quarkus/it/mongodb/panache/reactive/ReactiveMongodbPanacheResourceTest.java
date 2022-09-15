package io.quarkus.it.mongodb.panache.reactive;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkus.it.mongodb.panache.BookDTO;
import io.quarkus.it.mongodb.panache.book.BookDetail;
import io.quarkus.it.mongodb.panache.person.Person;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoReplicaSetTestResource;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(MongoReplicaSetTestResource.class)
@DisabledOnOs(OS.WINDOWS)
class ReactiveMongodbPanacheResourceTest {
    private static final TypeRef<List<BookDTO>> LIST_OF_BOOK_TYPE_REF = new TypeRef<List<BookDTO>>() {
    };
    private static final TypeRef<List<Person>> LIST_OF_PERSON_TYPE_REF = new TypeRef<List<Person>>() {
    };

    @Test
    public void testReactiveBookEntity() throws InterruptedException {
        callReactiveBookEndpoint("/reactive/books/entity");
    }

    @Test
    public void testReactiveBookRepository() throws InterruptedException {
        callReactiveBookEndpoint("/reactive/books/repository");
    }

    @Test
    public void testReactivePersonEntity() {
        callReactivePersonEndpoint("/reactive/persons/entity");
    }

    @Test
    public void testReactivePersonRepository() {
        callReactivePersonEndpoint("/reactive/persons/repository");
    }

    private void callReactiveBookEndpoint(String endpoint) throws InterruptedException {
        RestAssured.defaultParser = Parser.JSON;
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RestAssured.config
                .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((type, s) -> objectMapper));

        List<BookDTO> list = get(endpoint).as(LIST_OF_BOOK_TYPE_REF);
        assertEquals(0, list.size());

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
        assertEquals(201, response.statusCode());
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
        assertEquals(201, response.statusCode());

        list = get(endpoint).as(LIST_OF_BOOK_TYPE_REF);
        assertEquals(2, list.size());

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
        assertEquals(201, response.statusCode());

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
        assertEquals(202, response.statusCode());

        list = get(endpoint).as(LIST_OF_BOOK_TYPE_REF);
        assertEquals(4, list.size());

        //with sort
        list = get(endpoint + "?sort=author").as(LIST_OF_BOOK_TYPE_REF);
        assertEquals(4, list.size());

        // magic query find("author", author)
        list = get(endpoint + "/search/Victor Hugo").as(LIST_OF_BOOK_TYPE_REF);
        assertEquals(2, list.size());

        // magic query find("{'author':?1,'title':?1}", author, title)
        BookDTO book = get(endpoint + "/search?author=Victor Hugo&title=Notre-Dame de Paris").as(BookDTO.class);
        assertNotNull(book);

        // date
        book = get(endpoint + "/search?dateFrom=1885-01-01&dateTo=1887-01-01").as(BookDTO.class);
        assertNotNull(book);

        book = get(endpoint + "/search2?dateFrom=1885-01-01&dateTo=1887-01-01").as(BookDTO.class);
        assertNotNull(book);

        // magic query find("{'author'::author,'title'::title}", Parameters.with("author", author).and("title", title))
        book = get(endpoint + "/search2?author=Victor Hugo&title=Notre-Dame de Paris").as(BookDTO.class);
        assertNotNull(book);
        assertNotNull(book.getId());
        assertNotNull(book.getDetails());

        //update a book
        book.setTitle("Notre-Dame de Paris 2").setTransientDescription("should not be persisted");
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(book)
                .put(endpoint)
                .andReturn();
        assertEquals(202, response.statusCode());

        //check that the title has been updated and the transient description ignored
        book = get(endpoint + "/" + book.getId().toString()).as(BookDTO.class);
        assertEquals("Notre-Dame de Paris 2", book.getTitle());
        Assertions.assertNull(book.getTransientDescription());

        //delete a book
        response = RestAssured
                .given()
                .delete(endpoint + "/" + book.getId().toString())
                .andReturn();
        assertEquals(204, response.statusCode());

        list = get(endpoint).as(LIST_OF_BOOK_TYPE_REF);
        assertEquals(3, list.size());

        //test some special characters
        list = get(endpoint + "/search/Victor'\\ Hugo").as(LIST_OF_BOOK_TYPE_REF);
        assertEquals(0, list.size());

        //test SSE : there is no JSON serialization for SSE so the test is not very elegant ...
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8081" + endpoint + "/stream");
        try (SseEventSource source = SseEventSource.target(target).build()) {
            final IntegerAdder nbEvent = new IntegerAdder();
            source.register((inboundSseEvent) -> {
                try {
                    BookDTO theBook = objectMapper.readValue(inboundSseEvent.readData(), BookDTO.class);
                    assertNotNull(theBook);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                nbEvent.increment();
            });
            source.open();
            await().atMost(Duration.ofSeconds(10)).until(() -> nbEvent.count() == 3);
        }

        //delete all
        response = RestAssured
                .given()
                .delete(endpoint)
                .andReturn();
        Assertions.assertEquals(204, response.statusCode());
    }

    private void callReactivePersonEndpoint(String endpoint) {
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.config
                .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((type, s) -> new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));

        List<Person> list = get(endpoint).as(LIST_OF_PERSON_TYPE_REF);
        assertEquals(0, list.size());

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
        assertEquals(201, response.statusCode());

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
        assertEquals(204, response.statusCode());

        Person person4 = new Person();
        person4.id = 4L;
        person4.firstname = "Charles";
        person4.lastname = "Baudelaire";
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(person4)
                .patch(endpoint)
                .andReturn();
        assertEquals(202, response.statusCode());

        list = get(endpoint).as(LIST_OF_PERSON_TYPE_REF);
        assertEquals(4, list.size());

        //with sort
        list = get(endpoint + "?sort=firstname").as(LIST_OF_PERSON_TYPE_REF);
        assertEquals(4, list.size());

        //with project
        list = get(endpoint + "/search/Doe").as(LIST_OF_PERSON_TYPE_REF);
        Assertions.assertEquals(1, list.size());
        Assertions.assertNotNull(list.get(0).lastname);
        //expected the firstname field to be null as we project on lastname only
        Assertions.assertNull(list.get(0).firstname);

        //rename the Doe
        RestAssured
                .given()
                .queryParam("previousName", "Doe").queryParam("newName", "Dupont")
                .header("Content-Type", "application/json")
                .when().post(endpoint + "/rename")
                .then().statusCode(200);
        list = get(endpoint + "/search/Dupont").as(LIST_OF_PERSON_TYPE_REF);
        Assertions.assertEquals(1, list.size());

        //count
        Long count = get(endpoint + "/count").as(Long.class);
        assertEquals(4, count);

        //update a person
        person3.lastname = "Webster";
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(person3)
                .put(endpoint)
                .andReturn();
        assertEquals(202, response.statusCode());

        //check that the title has been updated
        person3 = get(endpoint + "/" + person3.id.toString()).as(Person.class);
        assertEquals(3L, person3.id);
        assertEquals("Webster", person3.lastname);

        //delete a person
        response = RestAssured
                .given()
                .delete(endpoint + "/" + person3.id.toString())
                .andReturn();
        assertEquals(204, response.statusCode());

        count = get(endpoint + "/count").as(Long.class);
        assertEquals(3, count);

        //delete all
        response = RestAssured
                .given()
                .delete(endpoint)
                .andReturn();
        assertEquals(204, response.statusCode());

        count = get(endpoint + "/count").as(Long.class);
        assertEquals(0, count);
    }

    private Date yearToDate(int year) {
        Calendar cal = new GregorianCalendar();
        cal.set(year, 1, 1);
        return cal.getTime();
    }

    private static class IntegerAdder {
        int cpt = 0;

        public void increment() {
            cpt++;
        }

        public int count() {
            return cpt;
        }
    }

    @Test
    public void testMoreEntityFunctionalities() {
        get("/test/reactive/entity").then().statusCode(200);
    }

    @Test
    public void testMoreRepositoryFunctionalities() {
        get("/test/reactive/repository").then().statusCode(200);
    }
}
